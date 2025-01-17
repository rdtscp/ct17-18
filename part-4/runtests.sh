#!/bin/bash
echo " --- Starting tests ---"
if [ -n "$1" ]; then
    echo "Running Test $1"
    $LLVM_DIR/bin/clang -S -emit-llvm -Xclang -disable-O0-optnone ./tests/test$1.c -o ./.temp/test_$1.ll 2>/dev/null
    $LLVM_DIR/bin/opt -S -mem2reg ./.temp/test_$1.ll -o ./.temp/test_$1.ll 2>/dev/null
    $LLVM_DIR/bin/opt -S -load ./passes/llvm-pass-simple-dce/build/skeleton/libSkeletonPass.so -skeletonpass ./.temp/test_$1.ll -o ./.temp/simp_out_test_$1.ll &> ./.temp/simp_log_test_$1
    $LLVM_DIR/bin/opt -S -load ./passes/llvm-pass-my-dce/build/skeleton/libSkeletonPass.so -skeletonpass ./.temp/test_$1.ll -o ./.temp/mydce_out_test_$1.ll &> ./.temp/mydce_log_test_$1
    echo "diff'ing  simple-dce and my-dce output"
    echo ""
    echo ""
    echo ""    
    diff -y ./.temp/simp_out_test_$1.ll ./.temp/mydce_out_test_$1.ll
else
    LLVM_DIR=~/Documents/CT/build
    TEST_NUM=0
    OUTPUT=""
    for filename in ./tests/*; do
        $LLVM_DIR/bin/clang -S -emit-llvm -Xclang -disable-O0-optnone $filename -o ./.temp/test_$TEST_NUM.ll 2>/dev/null
        $LLVM_DIR/bin/opt -S -mem2reg ./.temp/test_$TEST_NUM.ll -o ./.temp/test_$TEST_NUM.ll 2>/dev/null
        $LLVM_DIR/bin/opt -S -load ./passes/llvm-pass-simple-dce/build/skeleton/libSkeletonPass.so -skeletonpass ./.temp/test_$TEST_NUM.ll -o ./.temp/simp_out_test_$TEST_NUM.ll &> ./.temp/simp_log_test_$TEST_NUM
        $LLVM_DIR/bin/opt -S -load ./passes/llvm-pass-my-dce/build/skeleton/libSkeletonPass.so -skeletonpass ./.temp/test_$TEST_NUM.ll -o ./.temp/mydce_out_test_$TEST_NUM.ll &> ./.temp/mydce_log_test_$TEST_NUM
        
        diff ./.temp/simp_out_test_$TEST_NUM.ll ./.temp/mydce_out_test_$TEST_NUM.ll &>/dev/null
        if [ $? -ne 0 ]; then
            echo "Test $TEST_NUM Failed"
        else
            echo "Test $TEST_NUM Passed"
            $(rm ./.temp/test_$TEST_NUM.ll)
            $(rm ./.temp/mydce_log_test_$TEST_NUM)
            $(rm ./.temp/mydce_out_test_$TEST_NUM.ll)
            $(rm ./.temp/simp_log_test_$TEST_NUM)
            $(rm ./.temp/simp_out_test_$TEST_NUM.ll)
            
        fi
        let "TEST_NUM=TEST_NUM+1"
    done
fi