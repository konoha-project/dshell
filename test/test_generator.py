#!/usr/bin/env python

import os, shutil, re;

dshell_dir = "..";

if os.path.exists("./tests"):
	shutil.rmtree("./tests");
os.mkdir("./tests");
manuals = os.listdir(dshell_dir+"/manual/ja/");
for manual in manuals:
	if manual.endswith(".md"):
		manual_file = open(dshell_dir+"/manual/ja/"+manual, "r");
		test_group_name = manual.replace(".md", "");
		test_dir_path = "./tests/"+test_group_name;
		test_file = None;
		regex = re.compile("<pre(.+)title=\".+:\ *(.+\.ds)\"");
		for line in manual_file:
			match = regex.search(line);
			if match:
				if not os.path.exists(test_dir_path):
					os.mkdir(test_dir_path);
				test_file = open(test_dir_path+"/"+match.group(2), "w");
				continue;
			elif line.startswith("</pre>"):
				if test_file != None:
					test_file.close();
					test_file = None;
				continue;
			else:
				if test_file:
					test_file.write(line);
				continue;
