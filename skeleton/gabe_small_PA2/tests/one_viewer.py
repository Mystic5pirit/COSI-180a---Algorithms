#!/usr/bin/env python3
"""
one_viewer.py — View .one image files (raw, planar + Morton/row-major). No third-party deps.

By default:
  - Tries to open a Tk window to display the image (if tkinter is available).
  - Otherwise, exports a BMP and opens it with the OS default viewer.

Usage:
  python one_viewer.py input.one [--export out.bmp|out.ppm] [--no-open] [--composite-alpha]

.one v2 format expected (as produced by bmp_to_one.py):
  magic[4]      = b"ONE\\x02"
  layout[1]     = bit0: 1 = planar (required)
				  bits1-2: 0=row-major, 1=morton
  width[3], height[3]  (24-bit little-endian)
  channels[1]   = 3 or 4
  bitdepth[1]   = 8
  payload_len[4]= u32 LE
  reserved[2]   = 0
  payload[...]  = planar bytes: R-plane, G-plane, B-plane[, A-plane] in the given pixel order
  crc32[4]      = crc32(payload) LE
"""

import sys, os, struct, zlib, tempfile, platform, subprocess

MAGIC = b"ONE\x02"
ORDER_ROWMAJOR = 0
ORDER_MORTON   = 1

def from_24le(b: bytes) -> int:
	return b[0] | (b[1] << 8) | (b[2] << 16)

def _split_by_1bits(n: int) -> int:
	n &= 0xFFFF
	n = (n | (n << 8)) & 0x00FF00FF
	n = (n | (n << 4)) & 0x0F0F0F0F
	n = (n | (n << 2)) & 0x33333333
	n = (n | (n << 1)) & 0x55555555
	return n

def morton2D(x: int, y: int) -> int:
	return (_split_by_1bits(y) << 1) | _split_by_1bits(x)

def morton_indices(width: int, height: int):
	coords = []
	for y in range(height):
		for x in range(width):
			coords.append((morton2D(x, y), x, y))
	coords.sort(key=lambda t: t[0])
	return [(x, y) for _, x, y in coords]

def read_one(path: str):
	with open(path, "rb") as f:
		blob = f.read()
	p = 0
	if blob[p:p+4] != MAGIC:
		raise ValueError("Bad magic (expect ONE\\x02)")
	p += 4
	layout = blob[p]; p += 1
	planar = layout & 0x1
	order_code = (layout >> 1) & 0x3
	w = from_24le(blob[p:p+3]); p += 3
	h = from_24le(blob[p:p+3]); p += 3
	ch = blob[p]; p += 1
	bpc = blob[p]; p += 1
	if bpc != 8:
		raise ValueError("Only 8-bit per channel supported")
	payload_len = struct.unpack_from("<I", blob, p)[0]; p += 4
	p += 2  # reserved
	payload = blob[p:p+payload_len]; p += payload_len
	crc_expected = struct.unpack_from("<I", blob, p)[0]
	crc_actual = zlib.crc32(payload) & 0xFFFFFFFF
	if crc_actual != crc_expected:
		raise ValueError(f"CRC mismatch (expected {crc_expected:08x}, got {crc_actual:08x})")
	if not planar:
		raise ValueError("Viewer expects planar layout (bit0=1)")
	if ch not in (3,4):
		raise ValueError("channels must be 3 or 4")
	if len(payload) != w*h*ch:
		raise ValueError("Payload length does not match dimensions/channels")
	return {
		"width": w, "height": h, "channels": ch,
		"order": order_code, "payload": payload
	}

def to_row_major_rgb(order_code: int, w: int, h: int, ch: int, payload: bytes):
	# payload is planar in the given pixel order: [R-plane][G-plane][B-plane][A-plane?]
	N = w*h
	planes = []
	off = 0
	for _ in range(ch):
		planes.append(payload[off:off+N]); off += N
	# Build a mapping from order index -> (x,y)
	if order_code == ORDER_ROWMAJOR:
		order = [(x, y) for y in range(h) for x in range(w)]
	elif order_code == ORDER_MORTON:
		order = morton_indices(w, h)
	else:
		raise ValueError("Unsupported pixel order")
	# Reassemble interleaved row-major RGB(A)
	out = bytearray(N * ch)
	for idx, (x, y) in enumerate(order):
		di = (y*w + x) * ch
		for c in range(ch):
			out[di + c] = planes[c][idx]
	return bytes(out)

def composite_over_checker(rgb_or_rgba: bytes, w: int, h: int, ch: int, cell=8):
	if ch == 3:
		return rgb_or_rgba  # nothing to do
	rgba = rgb_or_rgba
	out = bytearray(w*h*3)
	for y in range(h):
		for x in range(w):
			R = rgba[(y*w + x)*4 + 0]
			G = rgba[(y*w + x)*4 + 1]
			B = rgba[(y*w + x)*4 + 2]
			A = rgba[(y*w + x)*4 + 3] / 255.0
			# checkerboard bg
			c = 200 if ((x//cell + y//cell) % 2 == 0) else 120
			Rb = Gb = Bb = c
			Ro = int(R*A + Rb*(1-A) + 0.5)
			Go = int(G*A + Gb*(1-A) + 0.5)
			Bo = int(B*A + Bb*(1-A) + 0.5)
			i = (y*w + x)*3
			out[i:i+3] = bytes((Ro,Go,Bo))
	return bytes(out)

def write_bmp(path: str, rgb: bytes, w: int, h: int):
	# Write 24-bit BMP (BGR, bottom-up, padded to 4-byte rows)
	row_raw = w*3
	row_stride = (row_raw + 3) & ~3
	image_size = row_stride * h
	file_size = 14 + 40 + image_size
	with open(path, "wb") as f:
		f.write(b"BM")
		f.write(struct.pack("<IHHI", file_size, 0, 0, 54))
		f.write(struct.pack("<IiiHHIIIIII",
			40, w, h, 1, 24, 0, image_size, 2835, 2835, 0, 0))
		# bottom-up rows
		pad = b"\x00" * (row_stride - row_raw)
		for y in range(h-1, -1, -1):
			row = rgb[y*w*3:(y+1)*w*3]
			# convert RGB -> BGR
			bgr = bytearray(len(row))
			for i in range(0, len(row), 3):
				r,g,b = row[i], row[i+1], row[i+2]
				bgr[i:i+3] = bytes((b,g,r))
			f.write(bgr)
			if pad:
				f.write(pad)

def write_ppm(path: str, rgb: bytes, w: int, h: int):
	with open(path, "wb") as f:
		f.write(f"P6\n{w} {h}\n255\n".encode("ascii"))
		f.write(rgb)

def try_open(path: str):
	try:
		if sys.platform.startswith("win"):
			os.startfile(path)  # type: ignore
		elif sys.platform == "darwin":
			subprocess.Popen(["open", path])
		else:
			subprocess.Popen(["xdg-open", path])
	except Exception:
		pass

def view_with_tk(rgb: bytes, w: int, h: int):
	try:
		import tkinter as tk
		from tkinter import ttk
		# Save a PPM to temp and load into PhotoImage
		import tempfile, os
		fd, ppm = tempfile.mkstemp(suffix=".ppm")
		os.close(fd)
		write_ppm(ppm, rgb, w, h)
		root = tk.Tk()
		root.title(f".one viewer — {w}x{h}")
		img = tk.PhotoImage(file=ppm)
		lbl = ttk.Label(root, image=img)
		lbl.pack()
		def on_close():
			try: os.remove(ppm)
			except: pass
			root.destroy()
		root.protocol("WM_DELETE_WINDOW", on_close)
		root.mainloop()
		return True
	except Exception:
		return False

def main():
	import argparse
	ap = argparse.ArgumentParser(description="View .one image (no external deps).")
	ap.add_argument("input_one")
	ap.add_argument("--export", help="Export to .bmp or .ppm at this path")
	ap.add_argument("--no-open", action="store_true", help="Do not auto-open a window/app")
	ap.add_argument("--composite-alpha", action="store_true", help="Composite RGBA over checkerboard for display/export")
	args = ap.parse_args()

	meta = read_one(args.input_one)
	w,h,ch = meta["width"], meta["height"], meta["channels"]
	rgb_or_rgba = to_row_major_rgb(meta["order"], w, h, ch, meta["payload"])

	# Prepare 24-bit RGB for display (composite if needed)
	if args.composite_alpha and ch == 4:
		rgb = composite_over_checker(rgb_or_rgba, w, h, ch=4)
	elif ch == 3:
		rgb = rgb_or_rgba
	else:
		# default: drop alpha (no composite)
		rgb = bytes(rgb_or_rgba[i] for i in range(0, len(rgb_or_rgba)) if (i % 4) != 3)

	# Export if requested
	if args.export:
		ext = os.path.splitext(args.export)[1].lower()
		if ext == ".bmp":
			write_bmp(args.export, rgb, w, h)
		elif ext == ".ppm":
			write_ppm(args.export, rgb, w, h)
		else:
			raise SystemExit("Unsupported export extension (use .bmp or .ppm)")
		if not args.no_open:
			try_open(args.export)
		print(f"Exported: {args.export}")
		return

	# Try tkinter; else write a temp BMP and open via OS
	if not args.no_open:
		if view_with_tk(rgb, w, h):
			return
		# Fallback
		import tempfile
		tmp = tempfile.mktemp(suffix=".bmp")
		write_bmp(tmp, rgb, w, h)
		try_open(tmp)
		print("Opened system viewer (BMP).")
	else:
		print(f"Read .one: {w}x{h}x{ch} (order={'morton' if meta['order']==1 else 'row'})")

if __name__ == "__main__":
	try:
		main()
	except Exception as e:
		print("ERROR:", e, file=sys.stderr)
		sys.exit(1)
