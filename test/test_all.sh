#!/usr/bin/env bash

REC=""
if [ $# -eq 1 ]; then
	REC="--rec $1"
fi

for test_dir in `ls ${TEST_DIR}/tests`; do
	for test_file in `ls ${TEST_DIR}/tests/$test_dir`; do
		echo "====== Test: ${test_file} ======"
		dshell $REC $TEST_DIR/tests/$test_dir/$test_file
	done
done
