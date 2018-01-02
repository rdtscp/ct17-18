#define DEBUG_TYPE "simpleDCE"
#include "llvm/Pass.h"
#include "llvm/IR/Function.h"
#include "llvm/Support/raw_ostream.h"
#include <map>
#include <vector>
using namespace llvm;
using namespace std;
namespace {
    struct SimpleDCE : public FunctionPass {
        SmallVector<Instruction*, 64> Worklist;
        static char ID;
        SimpleDCE() : FunctionPass(ID) {}
        virtual bool runOnFunction(Function &F) {
            errs() << "Function " << F.getName() << '\n';
            opCounter.clear();
            return false;
        }
    };
}
char SimpleDCE::ID = 0;
static RegisterPass<SimpleDCE> X("skeletonpass", "Simple dead code elimination");