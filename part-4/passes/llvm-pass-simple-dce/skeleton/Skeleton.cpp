#define DEBUG_TYPE "skeletonpass"
#include "llvm/Pass.h"
#include "llvm//Transforms/Utils/Local.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/Instruction.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/InstIterator.h"
#include <map>
#include <string>

#include <vector>
using namespace llvm;
using namespace std;
namespace {
    struct SimpleDCE : public FunctionPass {
        SmallVector<Instruction*, 64> Worklist;
        static char ID;
        SimpleDCE() : FunctionPass(ID) {}
        virtual bool runOnFunction(Function &F) {
            // Output Colours
            char blue[] = { 0x1b, '[', '1', ';', '3', '4', 'm', 0 };
            char normal[] = { 0x1b, '[', '0', ';', '3', '9', 'm', 0 };
            char red[] = { 0x1b, '[', '1', ';', '3', '1', 'm', 0 };


            errs() << "\nFunction " << F.getName() << '\n';
            bool changed;
            do {
                changed = false;
                // Find trivially dead instructions.
                for (auto bb = F.getBasicBlockList().rbegin(), e = F.getBasicBlockList().rend(); bb != e; ++bb) {
                    for (BasicBlock::reverse_iterator i = bb->rbegin(), e = bb->rend(); i != e; ++i) {
                        Instruction *currInst= &*i;
                        
                        bool isDead = isInstructionTriviallyDead(currInst);
                        if (isDead && !currInst->mayHaveSideEffects()) {
                            errs() << red;
                            Worklist.push_back(currInst);
                            changed = true;
                        }
                        
                        errs() << "\t";
                        currInst->print(errs());
                        errs() << normal << '\n';
                    }
                }

                // Remove trivially dead instructions, and its operands.
                for (int i=(Worklist.size() - 1); i >= 0; i--) {
                    Instruction *currInst = Worklist[i];
                    currInst->eraseFromParent();
                }
                Worklist.clear();
                errs() << "\n   ---   \n";
            } while (changed);

            
            return false;
        }
    };
    
}
char SimpleDCE::ID = 0;
static RegisterPass<SimpleDCE> X("skeletonpass", "Simple dead code elimination");