# Compile Test
$LLVM_DIR/bin/clang -S -emit-llvm -Xclang -disable-O0-optnone ../../../tests/dead.c -o test.ll

# Convert to SSA Form
$LLVM_DIR/bin/opt -S -mem2reg test.ll -o test.ll

# Run Pass
$LLVM_DIR/bin/opt -S -load skeleton/libSkeletonPass.so -skeletonpass test.ll -o test2.ll