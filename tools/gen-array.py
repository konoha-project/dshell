#!/usr/bin/python

import re
import sys
import os

gen_dir = './generated-array/dshell/lang'

class SourceBuilder:
	def __init__(self, namePrefix, typeName):
		self.prefix = namePrefix
		self.typeName = typeName
		self.line_buf = []

	def write(self):
		f = open(gen_dir + '/' + self.prefix + 'Array.java', 'w')
		f.write('// auto generated source file. do not edit me.\n')
		for line in self.line_buf:
			f.write(line)
		f.close()

	def append(self, line):
		line = line.replace('Generic', self.prefix).replace('Object', self.typeName)
		self.line_buf.append(line)


def main():
	if len(sys.argv) == 1:
		print 'need target file'
		sys.exit(1)

	target_file = sys.argv[1]
	f = open(target_file, 'r')
	lines = f.readlines()
	f.close()

	if not os.path.exists(gen_dir):
		os.makedirs(gen_dir)
	int_array_builder = SourceBuilder('Int', 'long')
	float_array_builder = SourceBuilder('Float', 'double')
	bool_array_builder = SourceBuilder('Boolean', 'boolean')
	for line in lines:
		if line.find('GenericClass') != -1:
			continue
		if line.startswith('import') and line.find('TypeParameter') != -1:
			continue

		line = line.replace('@TypeParameter()', '')
		int_array_builder.append(line)
		float_array_builder.append(line)
		bool_array_builder.append(line)

	int_array_builder.write()
	float_array_builder.write()
	bool_array_builder.write()

if __name__ == '__main__':
	main()
