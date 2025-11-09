'''
huffman .py

This module provides Huffman compression functionality that can be used by compression and decompression.
'''

import struct
import itertools

class Node:
	__slots__ = ("freq", "min_sym", "sym", "left", "right", "uid")
	_ids = itertools.count()
	
	def __init__(self, freq, sym=None, left=None, right=None):
		self.freq = freq
		self.sym = sym
		self.left = left
		self.right = right

		if self.left == None and self.right == None:
			# leaf node
			self.min_sym = sym
		else:
			# internal node
			self.min_sym = min(self.left.min_sym, self.right.min_sym)
		
		self.uid = next(Node._ids)

	
	def __lt__(self, other):
		if self.freq < other.freq:
			return True
		elif self.freq > other.freq:
			return False
		elif self.min_sym < other.min_sym:
			return True
		elif self.min_sym > other.min_sym:
			return False
		elif self.uid < other.uid:
			return True
		elif self.uid > other.uid:
			return False
		pass
		
def build_tree(freqs):
	nodes = []
	for byte, freq in enumerate(freqs):
		if freq != 0:
			nodes.append(Node(freq, sym=byte))
	while (len(nodes) > 1):
		# From what I can find online, bisect is probably the better way, but I don't know if I can use that
		nodes.sort(reverse=True)
		left = nodes.pop()
		right = nodes.pop()
		new_node = Node(freq=left.freq+right.freq, left=left, right=right)
		nodes.append(new_node)
	if len(nodes) == 0:
		return
	return nodes.pop()
	
def derive_codes(root, freqs):
	def dfs(node, code_string):
		if node == None:
			return
		if node.left == None and node.right == None:
			# leaf node
			output[node.sym] = code_string
		else:
			# internal node
			dfs(node=node.left, code_string=code_string + '0')
			dfs(node=node.right, code_string=code_string + '1')

	output = {}
	dfs(root, '')
	if len(output) == 1:
		# If there is only one element then it should have the code '0' not ''
		return {root.sym:'0'}
	else:
		return output


def count_frequencies(path):
	CHUNK_SIZE = 1 << 20  # 1 to power of 20 bytes (1MB)
	freqs = [0] * 256
	total_size = 0
	with open(path, 'rb') as file:
		chunk = file.read(CHUNK_SIZE)
		while chunk:
			total_size += len(chunk)
			for byte in chunk:
				freqs[byte] += 1
			chunk = file.read(CHUNK_SIZE)
	return freqs, total_size

# Helper functions
# Do not modify these functions
def write_u64be(f, n):
	f.write(struct.pack(">Q", n))

def write_u32be(f, n):
	f.write(struct.pack(">I", n))

def read_u64be(f):
	return struct.unpack(">Q", f.read(8))[0]

def read_u32be(f):
	return struct.unpack(">I", f.read(4))[0]
   
def write_compressed_data(in_path, codes, fout):
	CHUNK_SIZE = 1 << 20
	bitbuf = 0
	bitcount = 0
	with open(in_path, "rb") as fin:
		while True:
			chunk = fin.read(CHUNK_SIZE)
			if not chunk:
				break
			for b in chunk:
				code_string = codes[b]
				for i in range(len(code_string)):
					bit = int(code_string[i])
					bitbuf = (bitbuf << 1) | bit
					bitcount += 1
					if bitcount == 8:
						fout.write(bytes([bitbuf & 0xFF]))
						bitbuf = 0
						bitcount = 0
	if bitcount > 0:
		bitbuf <<= (8 - bitcount)
		fout.write(bytes([bitbuf & 0xFF]))

def read_compressed_data(f, root, size, out):
	if size == 0 or root is None:
		return
	if root.sym is not None:
		block = bytes([root.sym]) * min(size, 1_048_576)
		remain = size
		while remain > 0:
			n = min(remain, len(block))
			out.write(block[:n])
			remain -= n
		return
	node = root
	bytes_written = 0   
	remaining = size
	while remaining > 0:
		byte = f.read(1)
		if not byte:
			cur = 0
		else:
			cur = byte[0]
		for bitpos in range(7, -1, -1):
			bit = (cur >> bitpos) & 1
			node = node.right if bit else node.left
			if node.sym is not None:
				out.write(bytes([node.sym]))
				remaining -= 1
				bytes_written += 1
				if remaining == 0:
					return
				node = root
	return

def decompress_file(in_path, out_path, magic, file_type_name):
	with open(in_path, "rb") as f:
		head = f.read(4)
		if head != magic:
			raise ValueError("Bad magic: expected 'ANT0'")
		size = read_u64be(f)
		freqs = [read_u32be(f) for _ in range(256)]
		root = build_tree(freqs)
		with open(out_path, "wb") as out:
			read_compressed_data(f, root, size, out)
			
def compress_file(in_path, out_path, magic, file_type_name, chunk_size=1 << 20):
	freqs, total_size = count_frequencies(in_path)
	root = build_tree(freqs)
	codes = derive_codes(root, freqs)
	with open(out_path, "wb") as fout:
		fout.write(magic)
		write_u64be(fout, total_size)
		for i in range(256):
			write_u32be(fout, freqs[i])
		write_compressed_data(in_path, codes, fout)