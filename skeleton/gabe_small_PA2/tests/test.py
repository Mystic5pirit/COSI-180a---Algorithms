#!/usr/bin/env python3
"""
test.py — Comprehensive tests for Huffman compression core functions.

This module tests the core Huffman compression functionality including:
- Node class initialization and comparison
- build_tree function
- derive_codes function  
- count_frequencies function

The tests verify both basic functionality and edge cases.
"""

import sys
import os
import tempfile

# Add parent directory to path to import huffman module
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from src.huffman import Node, build_tree, derive_codes, count_frequencies


def test_node_initialization():
	"""Test Node class initialization for both leaf and internal nodes."""
	print("Testing Node initialization...")
	
	# Test leaf node
	leaf = Node(5, sym=65)  # 'A' with frequency 5
	assert leaf.freq == 5, f"Expected freq=5, got {leaf.freq}"
	assert leaf.sym == 65, f"Expected sym=65, got {leaf.sym}"
	assert leaf.min_sym == 65, f"Expected min_sym=65, got {leaf.min_sym}"
	assert leaf.left is None, f"Expected left=None for leaf node"
	assert leaf.right is None, f"Expected right=None for leaf node"
	assert hasattr(leaf, 'uid'), "Node should have uid attribute"
	
	# Test internal node
	left_child = Node(3, sym=66)   # 'B' with frequency 3
	right_child = Node(2, sym=67)  # 'C' with frequency 2
	internal = Node(5, left=left_child, right=right_child)
	
	assert internal.freq == 5, f"Expected freq=5, got {internal.freq}"
	assert internal.sym is None, f"Expected sym=None for internal node"
	assert internal.min_sym == 66, f"Expected min_sym=66 (min of 66,67), got {internal.min_sym}"
	assert internal.left is left_child, "Left child should be set correctly"
	assert internal.right is right_child, "Right child should be set correctly"
	assert hasattr(internal, 'uid'), "Node should have uid attribute"
	
	print("✓ Node initialization tests passed")


def test_node_comparison():
	"""Test Node comparison for deterministic ordering."""
	print("Testing Node comparison...")
	
	# Test frequency ordering (lower frequency comes first)
	node1 = Node(1, sym=65)  # freq=1
	node2 = Node(2, sym=66)  # freq=2
	assert node1 < node2, "Node with lower frequency should be less than node with higher frequency"
	assert not (node2 < node1), "Node with higher frequency should not be less than node with lower frequency"
	
	# Test min_sym ordering when frequencies are equal
	node3 = Node(1, sym=65)  # min_sym=65
	node4 = Node(1, sym=66)  # min_sym=66
	assert node3 < node4, "Node with lower min_sym should be less when frequencies are equal"
	
	# Test uid ordering when both frequency and min_sym are equal
	node5 = Node(1, sym=65)  # Should get uid=0
	node6 = Node(1, sym=65)  # Should get uid=1
	assert node5 < node6, "Node with lower uid should be less when frequency and min_sym are equal"
	
	print("✓ Node comparison tests passed")


def test_build_tree_empty():
	"""Test build_tree with empty frequency list."""
	print("Testing build_tree with empty frequencies...")
	
	freqs = [0] * 256
	root = build_tree(freqs)
	assert root is None, "build_tree should return None for empty frequencies"
	
	print("✓ Empty tree test passed")


def test_build_tree_single_symbol():
	"""Test build_tree with single symbol."""
	print("Testing build_tree with single symbol...")
	
	freqs = [0] * 256
	freqs[65] = 10  # Only 'A' appears
	root = build_tree(freqs)
	
	assert root is not None, "build_tree should return a node for single symbol"
	assert root.sym == 65, f"Expected sym=65, got {root.sym}"
	assert root.freq == 10, f"Expected freq=10, got {root.freq}"
	assert root.left is None, "Single symbol node should have no left child"
	assert root.right is None, "Single symbol node should have no right child"
	
	print("✓ Single symbol tree test passed")


def test_build_tree_multiple_symbols():
	"""Test build_tree with multiple symbols."""
	print("Testing build_tree with multiple symbols...")
	
	freqs = [0] * 256
	freqs[65] = 5  # 'A' appears 5 times
	freqs[66] = 3  # 'B' appears 3 times  
	freqs[67] = 2  # 'C' appears 2 times
	
	root = build_tree(freqs)
	
	assert root is not None, "build_tree should return a node for multiple symbols"
	assert root.freq == 10, f"Expected total freq=10, got {root.freq}"
	assert root.sym is None, "Root should be internal node (no symbol)"
	assert root.left is not None, "Root should have left child"
	assert root.right is not None, "Root should have right child"
	
	# Verify that all symbols are reachable
	symbols_found = set()
	
	def collect_symbols(node):
		if node.sym is not None:
			symbols_found.add(node.sym)
		else:
			collect_symbols(node.left)
			collect_symbols(node.right)
	
	collect_symbols(root)
	expected_symbols = {65, 66, 67}
	assert symbols_found == expected_symbols, f"Expected symbols {expected_symbols}, found {symbols_found}"
	
	print("✓ Multiple symbols tree test passed")


def test_derive_codes_empty():
	"""Test derive_codes with empty tree."""
	print("Testing derive_codes with empty tree...")
	
	freqs = [0] * 256
	codes = derive_codes(None, freqs)
	assert codes == {}, f"Expected empty dict, got {codes}"
	
	print("✓ Empty codes test passed")


def test_derive_codes_single_symbol():
	"""Test derive_codes with single symbol."""
	print("Testing derive_codes with single symbol...")
	
	freqs = [0] * 256
	freqs[65] = 10  # Only 'A' appears
	root = build_tree(freqs)
	codes = derive_codes(root, freqs)
	
	assert len(codes) == 1, f"Expected 1 code, got {len(codes)}"
	assert 65 in codes, "Code for symbol 65 should exist"
	assert codes[65] == '0', f"Expected code '0' for single symbol, got '{codes[65]}'"
	
	print("✓ Single symbol codes test passed")


def test_derive_codes_multiple_symbols():
	"""Test derive_codes with multiple symbols."""
	print("Testing derive_codes with multiple symbols...")
	
	freqs = [0] * 256
	freqs[65] = 5  # 'A' appears 5 times
	freqs[66] = 3  # 'B' appears 3 times
	freqs[67] = 2  # 'C' appears 2 times
	
	root = build_tree(freqs)
	codes = derive_codes(root, freqs)
	
	assert len(codes) == 3, f"Expected 3 codes, got {len(codes)}"
	assert all(sym in codes for sym in [65, 66, 67]), "All symbols should have codes"
	
	# Verify codes are strings of 0s and 1s
	for sym, code in codes.items():
		assert isinstance(code, str), f"Code should be string, got {type(code)}"
		assert all(c in '01' for c in code), f"Code should contain only 0s and 1s, got '{code}'"
	
	# Verify no code is a prefix of another (prefix-free property)
	code_list = list(codes.values())
	for i, code1 in enumerate(code_list):
		for j, code2 in enumerate(code_list):
			if i != j and (code1.startswith(code2) or code2.startswith(code1)):
				assert False, f"Codes '{code1}' and '{code2}' violate prefix-free property"
	
	print("✓ Multiple symbols codes test passed")


def test_count_frequencies():
	"""Test count_frequencies function."""
	print("Testing count_frequencies...")
	
	# Create a temporary file with known content
	test_content = b"Hello, World!"
	with tempfile.NamedTemporaryFile(mode='wb', delete=False) as f:
		f.write(test_content)
		temp_file = f.name
	
	try:
		freqs, size = count_frequencies(temp_file)
		
		# Check file size
		assert size == len(test_content), f"Expected size {len(test_content)}, got {size}"
		
		# Check frequency counts
		expected_freqs = [0] * 256
		for byte in test_content:
			expected_freqs[byte] += 1
		
		assert freqs == expected_freqs, f"Frequency counts don't match expected values"
		
		# Verify some specific counts
		assert freqs[ord('H')] == 1, f"Expected 'H' count=1, got {freqs[ord('H')]}"
		assert freqs[ord('l')] == 3, f"Expected 'l' count=3, got {freqs[ord('l')]}"
		assert freqs[ord('o')] == 2, f"Expected 'o' count=2, got {freqs[ord('o')]}"
		
		print("✓ count_frequencies test passed")
		
	finally:
		# Clean up temporary file
		os.unlink(temp_file)


def test_integration():
	"""Test integration of all functions together."""
	print("Testing integration of all functions...")
	
	# Create test data
	test_content = b"AABBBCCCC"  # A=2, B=3, C=4
	with tempfile.NamedTemporaryFile(mode='wb', delete=False) as f:
		f.write(test_content)
		temp_file = f.name
	
	try:
		# Test complete workflow
		freqs, size = count_frequencies(temp_file)
		root = build_tree(freqs)
		codes = derive_codes(root, freqs)
		
		# Verify results
		assert size == len(test_content), f"Expected size {len(test_content)}, got {size}"
		assert freqs[ord('A')] == 2, "A should appear 2 times"
		assert freqs[ord('B')] == 3, "B should appear 3 times"
		assert freqs[ord('C')] == 4, "C should appear 4 times"
		
		assert root is not None, "Tree should be built successfully"
		assert len(codes) == 3, "Should have codes for 3 symbols"
		
		# Verify that codes make sense (more frequent symbols should have shorter codes)
		# C appears most (4 times), so should have shortest code
		# A appears least (2 times), so should have longest code
		c_code = codes[ord('C')]
		b_code = codes[ord('B')]
		a_code = codes[ord('A')]
		
		assert len(c_code) <= len(b_code) <= len(a_code), "More frequent symbols should have shorter codes"
		
		print("✓ Integration test passed")
		
	finally:
		# Clean up temporary file
		os.unlink(temp_file)


def run_all_tests():
	"""Run all tests and report results."""
	print("=" * 60)
	print("HUFFMAN COMPRESSION CORE FUNCTIONS TEST SUITE")
	print("=" * 60)
	
	tests = [
		test_node_initialization,
		test_node_comparison,
		test_build_tree_empty,
		test_build_tree_single_symbol,
		test_build_tree_multiple_symbols,
		test_derive_codes_empty,
		test_derive_codes_single_symbol,
		test_derive_codes_multiple_symbols,
		test_count_frequencies,
		test_integration
	]
	
	passed = 0
	failed = 0
	
	for test in tests:
		try:
			test()
			passed += 1
		except Exception as e:
			print(f"✗ {test.__name__} failed: {e}")
			failed += 1
		print()
	
	print("=" * 60)
	print(f"TEST RESULTS: {passed} passed, {failed} failed")
	print("=" * 60)
	
	if failed == 0:
		print("All tests passed!")
		return True
	else:
		print(f"❌ {failed} test(s) failed!")
		return False


if __name__ == "__main__":
	success = run_all_tests()
	sys.exit(0 if success else 1)
