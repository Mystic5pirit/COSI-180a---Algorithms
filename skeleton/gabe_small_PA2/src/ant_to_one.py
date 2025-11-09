import sys, os
from huffman import decompress_file

MAGIC = b"ANT0"

def decompress_ant_to_bytes(in_path, out_path, chunk_size=1<<20):
	decompress_file(in_path, out_path, MAGIC, "ANT")

def main():
	if len(sys.argv) != 3:
		print("Usage: python ant_to_one.py input.ant output.one", file=sys.stderr)
		sys.exit(2)
	in_path, out_path = sys.argv[1], sys.argv[2]
	if not os.path.exists(in_path):
		print(f"Input not found: {in_path}", file=sys.stderr)
		sys.exit(1)
	try:
		decompress_ant_to_bytes(in_path, out_path)
		print(f"Wrote: {out_path}")
	except ValueError as e:
		print(f"Error: {e}", file=sys.stderr)
		sys.exit(1)
	except Exception as e:
		print(f"Unexpected error: {e}", file=sys.stderr)
		sys.exit(1)

if __name__ == "__main__":
	main()