#!/bin/bash
echo " --- Starting tests ---"
LLVM_DIR=~/Documents/CT/build
TEST_NUM=0
OUTPUT=""
for filename in ./tests/*; do
    $LLVM_DIR/bin/clang -S -emit-llvm -Xclang -disable-O0-optnone $filename -o ./.temp/test_$TEST_NUM.ll 2>/dev/null
    $LLVM_DIR/bin/opt -S -mem2reg ./.temp/test_$TEST_NUM.ll -o ./.temp/test_$TEST_NUM.ll 2>/dev/null
    $LLVM_DIR/bin/opt -S -load ./passes/llvm-pass-simple-dce/build/skeleton/libSkeletonPass.so -skeletonpass ./.temp/test_$TEST_NUM.ll -o ./.temp/simp_out_test_$TEST_NUM.ll 1> ./.temp/simp_log_test_$TEST_NUM 2>/dev/null
    $LLVM_DIR/bin/opt -S -load ./passes/llvm-pass-my-dce/build/skeleton/libSkeletonPass.so -skeletonpass ./.temp/test_$TEST_NUM.ll -o ./.temp/mydce_out_test_$TEST_NUM.ll 1> ./.temp/mydce_log_test_$TEST_NUM 2>/dev/null
    
    diff ./.temp/simp_out_test_$TEST_NUM.ll ./.temp/mydce_out_test_$TEST_NUM.ll &>/dev/null
    if [ $? -ne 0 ]; then
        OUTPUT=$OUTPUT"\n[Test $TEST_NUM]" 
    fi
    let "TEST_NUM=TEST_NUM+1"
done
echo "Failed Tests:"
echo $OUTPUT