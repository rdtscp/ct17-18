#define DEBUG_TYPE "skeletonpass"
#include "llvm/Pass.h"
#include "llvm/IR/BasicBlock.h"
#include "llvm//Transforms/Utils/Local.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/Instruction.h"
#include "llvm/IR/Instructions.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/InstIterator.h"
#include <map>
#include <string>
#include <vector>
#include <algorithm>
using namespace llvm;
using namespace std;
namespace {

    struct SimpleDCE : public FunctionPass {

        struct LivenessBlock {
            Instruction *i;
            SmallVector<StringRef, 64> in_set;
            SmallVector<StringRef, 64> out_set;
        };

        SmallVector<Instruction*, 64> Worklist;          // Instructions to remove list.
        SmallVector<LivenessBlock, 64> lastLivePass;     // Stores temp state of all Instructions IN/OUT sets.
        SmallVector<LivenessBlock, 64> instLiveness;     // List of Instruction Liveness data.

        SmallVector<StringRef, 64> phiVars;              // List of Variables used in PHI instructions.
        

        static char ID;
        SimpleDCE() : FunctionPass(ID) {}
        virtual bool runOnFunction(Function &F) {
            // Output Colours
            char blue[] = { 0x1b, '[', '1', ';', '3', '4', 'm', 0 };
            char red[] = { 0x1b, '[', '1', ';', '3', '1', 'm', 0 };
            char mag[] = { 0x1b, '[', '1', ';', '3', '5', 'm', 0 };
            char green[] = { 0x1b, '[', '1', ';', '3', '2', 'm', 0 };
            char normal[] = { 0x1b, '[', '0', ';', '3', '9', 'm', 0 };

            bool instrRemoved;
            // // Do Liveness Analysis on entire Program and remove instructions.
            do {
                instrRemoved = false;

                // Generate List of instLiveness of all Instructions.
                for (auto bb = F.getBasicBlockList().rbegin(), e = F.getBasicBlockList().rend(); bb != e; ++bb) {
                    for (BasicBlock::reverse_iterator i = bb->rbegin(), e = bb->rend(); i != e; ++i) {
                        // Declare all members.
                        Instruction *currInst= &*i;
                        SmallVector<StringRef, 64> currInst_in_set;
                        SmallVector<StringRef, 64> currInst_out_set;

                        // Initialize this instruction liveness.
                        LivenessBlock currInstLiveness;
                        currInstLiveness.i = currInst;
                        currInstLiveness.in_set = currInst_in_set;
                        currInstLiveness.out_set = currInst_out_set;

                        // Push to list.
                        instLiveness.push_back(currInstLiveness);
                    }
                }
                // Copy initialised data to temp.
                lastLivePass = instLiveness;

                bool livenessCalculating;
                errs() << mag << "\n //------ Calculating Liveness ------\\\\\n" << normal;
                do {
                    livenessCalculating = false;

                    // Compute the IN/OUT sets for all Instructions in all BasicBlocks.
                    for (auto bb = F.getBasicBlockList().rbegin(), e = F.getBasicBlockList().rend(); bb != e; ++bb) {
                        errs() << mag << "\n\n|---------[ --> " << bb->getName() << " <-- ]---------|";
                        // Calculate IN/OUT sets for all Instructions in this BasicBlock.
                        calculateInOutSets(&*bb);
                    }

                    // Check that the temp IN/OUT's are the same as the most recent IN/OUT's
                    for (LivenessBlock tempBlock: lastLivePass) {
                        LivenessBlock currBlock = getInstLiveness(tempBlock.i);
                        // If the temp's IN/OUT is not the same as the IN/OUT from last pass, recalculate.
                        if (!setsEqual(&currBlock.in_set, &tempBlock.in_set) || !setsEqual(&currBlock.out_set, &tempBlock.out_set)) {
                            livenessCalculating = true;
                        }
                    }
                } while (livenessCalculating);
                errs() << mag << "\n \\\\----------------------------------//\n" << normal;                
                printLiveness();
                

                // Loop through all Instructions in the Program, and mark dead Instructions for deletion.
                for (auto bb = F.getBasicBlockList().rbegin(), e = F.getBasicBlockList().rend(); bb != e; ++bb) {
                    // Loop through all Instructions in the current BasicBlock.
                    for (BasicBlock::reverse_iterator i = bb->rbegin(), e = bb->rend(); i != e; ++i) {
                        // Get Data required.
                        Instruction *currInst = &*i;
                        SmallVector<StringRef, 64> currInstDEF = getDef(currInst);
                        SmallVector<StringRef, 64> currInstOUT = getInstLiveness(currInst).out_set;

                        // If this Instruction does not have a DEF, skip it.
                        if (currInstDEF.size() == 0) continue;
                        StringRef instDef = currInstDEF[0];
                        
                        // If this Instructions DEF is not in its OUT set, mark it for removal.
                        if (!setContains(instDef, &currInstOUT)) Worklist.push_back(currInst);
                    }
                }

                // Remove Dead Instructions.
                
                errs() << red <<  "\n --- Removing Instructions ---\n";
                for (Instruction *i: Worklist) {
                    errs() << "\n";
                    if (safeToRemove(i, &phiVars)) {
                        errs() << red;
                        i->print(errs());
                        errs() << normal;
                        instrRemoved = true;
                        i->eraseFromParent();
                    }
                    else {
                        errs() << green;
                        i->print(errs());
                        errs() << normal;
                    }
                }
                Worklist.clear();
                lastLivePass.clear();
                instLiveness.clear();
                
                errs() << red <<  "\n\n -----------------------------" << normal;
            } while (instrRemoved);
            
            return false;
        }

        void calculateInOutSets(BasicBlock *bb) {
            // Output Colours
            char blue[] = { 0x1b, '[', '1', ';', '3', '4', 'm', 0 };
            char normal[] = { 0x1b, '[', '0', ';', '3', '9', 'm', 0 };

            // Loop through Instructions in reverse, calculating IN/OUT sets until convergence.
            int temp = 0;
            bool changed;
            int inst_num = bb->size() - 1;
            for (BasicBlock::reverse_iterator i = bb->rbegin(), e = bb->rend(); i != e; ++i, inst_num--) {
                // Get the current Instruction.
                Instruction *currInst= &*i;

                errs() << blue << "\n\n";
                currInst->print(errs());
                errs() << normal;

                // Get USE/DEF
                SmallVector<StringRef, 64> use_set = getUse(currInst);
                SmallVector<StringRef, 64> def_set = getDef(currInst);
            
                // Store the current instructions IN and OUT sets.
                SmallVector<StringRef, 64> in_temp   = getInstLiveness(currInst).in_set;
                SmallVector<StringRef, 64> out_temp  = getInstLiveness(currInst).out_set;
                printSet(&in_temp, "IN_t  = ");
                printSet(&out_temp, "OUT_t = ");

                // Store to global temp structure.
                lastLivePass[getTempInstLiveIdx(currInst)].in_set  = in_temp;
                lastLivePass[getTempInstLiveIdx(currInst)].out_set = out_temp;

                // Calculate new IN/OUT sets.
                SmallVector<StringRef, 64> new_in;
                SmallVector<StringRef, 64> new_out;

                // If this is a BranchInst, can have multiple successors.
                if (isa<BranchInst>(currInst)) {
                    // Do something with all successors.
                    // out[n] = UNION in[n+1]
                    for (int j=0; j < cast<BranchInst>(currInst)->getNumSuccessors(); j++) {
                        BasicBlock *branchBlock = cast<BranchInst>(currInst)->getSuccessor(j);
                        LivenessBlock branchInst = getInstLiveness(&branchBlock->front());
                        for (StringRef inVar: branchInst.in_set) {
                            if (!setContains(inVar, &new_out)) new_out.push_back(inVar);
                        }
                    }
                    // in[n] = use[n]
                    new_in  = use_set;
                    // in[n] += (out[n] - def[n]) [EXCLUDING Duplicates]
                    for (StringRef currVar: new_out) {
                        if (!setContains(currVar, &def_set) && !setContains(currVar, &new_in)) {
                            new_in.push_back(currVar);
                        }
                    }
                }
                else if (currInst->isTerminator()) {
                    // Do something with next block instruction.
                    // in[n] = use[n]
                    new_in  = use_set;
                }
                else {
                    // Peek next Instruction to get its IN set.
                    Instruction *nextInst= &*(i->getNextNode());
                    
                    new_out = getInstLiveness(nextInst).in_set;  // out[n] = UNION of in[n+1]
                    new_in  = use_set;                           // in[n] = use[n]
                    for (StringRef currVar: new_out) {           // in[n] += (out[n] - def[n]) [EXCLUDING Duplicates]
                        if (!setContains(currVar, &def_set) && !setContains(currVar, &new_in)) {
                            new_in.push_back(currVar);
                        }
                    }
                }
                printSet(&new_in, "IN    = ");
                printSet(&new_out, "OUT   = ");

                // Update our Liveness model.
                instLiveness[getInstLivenessIdx(currInst)].in_set  = new_in;
                instLiveness[getInstLivenessIdx(currInst)].out_set = new_out;
            }
        }

        SmallVector<StringRef, 64> getUse(Instruction *i) {
            // Get the USE of this instruction.
            SmallVector<StringRef, 64> output;
            errs() << "\nUSE   = [ ";
            for (Use &U : i->operands()) {
                Value *v = U.get();
                if (v->getName() != "") {
                    errs() << v->getName() << ", ";
                    output.push_back(v->getName());
                }
            }
            errs() << "]";
            return output;
        }

        SmallVector<StringRef, 64> getDef(Instruction *i) {
            // Get the DEF of this instruction.
            SmallVector<StringRef, 64> output;
            errs() << "\nDEF   = [ ";
            if (i->getName() != "") {
                output.push_back(i->getName());
                errs() << i->getName();
            }
            errs() << " ]";
            return output;
        }

        int getInstLivenessIdx(Instruction *currInst) {
            int i=0;
            for (LivenessBlock currInstLiveness: instLiveness) {
                if (currInstLiveness.i == currInst) return i;
                i++;
            }
            return -1;
        }

        int getTempInstLiveIdx(Instruction *currInst) {
            int i=0;
            for (LivenessBlock currInstLiveness: lastLivePass) {
                if (currInstLiveness.i == currInst) return i;
                i++;
            }
            return -1;
        }

        LivenessBlock getInstLiveness(Instruction *currInst) {
            for (LivenessBlock currInstLiveness: instLiveness) {
                if (currInstLiveness.i == currInst) return currInstLiveness;
            }
            LivenessBlock output;
            output.i = NULL;
            return output;
        }

        LivenessBlock getTempInstLiveness(Instruction *currInst) {
            for (LivenessBlock currInstLiveness: lastLivePass) {
                if (currInstLiveness.i == currInst) return currInstLiveness;
            }
            LivenessBlock output;
            output.i = NULL;
            return output;
        }        

        SmallVector<StringRef, 64> updatePHIVars(Instruction *i, SmallVector<StringRef, 64> *phiVars) {
            // Get the StringRef's used by this PHI.
            SmallVector<StringRef, 64> instVars;
            for (Use &U: i->operands()) {
                Value *v = U.get();
                if (v->getName() != "") {
                    instVars.push_back(v->getName());
                }
            }
            // Generate the output SmallVector.
            SmallVector<StringRef, 64> output;
            for (StringRef var: *phiVars) {
                if (!setContains(var, &instVars)) {
                    output.push_back(var);
                }
            }
            return output;
        }

        bool safeToRemove(Instruction *i, SmallVector<StringRef, 64> *phiVars) {
            // Output Colours
            char blue[] = { 0x1b, '[', '1', ';', '3', '4', 'm', 0 };
            char red[] = { 0x1b, '[', '1', ';', '3', '1', 'm', 0 };
            char mag[] = { 0x1b, '[', '1', ';', '3', '5', 'm', 0 };
            char green[] = { 0x1b, '[', '1', ';', '3', '3', 'm', 0 };
            char normal[] = { 0x1b, '[', '0', ';', '3', '9', 'm', 0 };

            if (i->isTerminator()) { errs() << green << "isTerminator()\t\t"; return false; }
            if (i->mayHaveSideEffects()) { errs() << green<< "mayHaveSideEffects\t"; return false; }
            if (isa<ReturnInst>(i) || isa<SwitchInst>(i) || isa<BranchInst>(i) || isa<IndirectBrInst>(i) || isa<CallInst>(i)) { errs() << green<< "isa<OTHER>(i)\t\t";  return false; }
            if (isa<StoreInst>(i)) { errs() << green<< "isa<StoreInst>(i)\t\t";  return false; }
            return true;
        }

        bool setsEqual(SmallVector<StringRef, 64> *set_a, SmallVector<StringRef, 64> *set_b) {
            return std::is_permutation(set_a->begin(), set_a->end(), set_b->begin());
        }

        // Returns if a StringRef[target] (i.e. a variable) exists in a Set[set].
        bool setContains(StringRef target, SmallVector<StringRef, 64> *set) {
            bool output = false;
            for (StringRef *elem = set->begin(); elem != set->end(); ++elem) {
                if (elem->str() == target.str()) output = true;
            }
            return output;
        }


        // HELPERS

        void printLiveness() {
            // Output Colours
            char blue[] = { 0x1b, '[', '1', ';', '3', '4', 'm', 0 };
            char normal[] = { 0x1b, '[', '0', ';', '3', '9', 'm', 0 };
            errs() << "\n ########### DUMPING LIVENESS ########### ";
            for (LivenessBlock lb: instLiveness) {
                errs() << "\n" << blue;
                lb.i->print(errs());
                errs() << normal;
                printIN(&lb.in_set);
                printOUT(&lb.out_set);
            }
            errs() << "\n ######################################## ";
        }

        void printIN(SmallVector<StringRef, 64> *in_set) {
            // Output the sets.
            errs() << "\n\t  - IN:\t\t\t[ ";
            for (StringRef *curr_var = in_set->begin(); curr_var != in_set->end(); ++curr_var) {
                errs() << curr_var->str()  << ", ";
            }
            errs() << "]";
        }

        void printOUT(SmallVector<StringRef, 64> *in_set) {
            // Output the sets.
            errs() << "\n\t  - OUT:\t\t[ ";
            for (StringRef *curr_var = in_set->begin(); curr_var != in_set->end(); ++curr_var) {
                errs() << curr_var->str()  << ", ";
            }
            errs() << "]";
        }

        void printInOut(LivenessBlock *currBlock, LivenessBlock *tempBlock) {
            // Output Colours
            char blue[] = { 0x1b, '[', '1', ';', '3', '4', 'm', 0 };
            char normal[] = { 0x1b, '[', '0', ';', '3', '9', 'm', 0 };


            errs() << "\n" << blue;
            currBlock->i->print(errs());
            errs() << normal;

            // Output the sets.
            errs() << "\n\t  - IN:\t\t\t[ ";
            for (StringRef *curr_var = currBlock->in_set.begin(); curr_var != currBlock->in_set.end(); ++curr_var) {
                errs() << curr_var->str()  << ", ";
            }
            errs() << "]";
            errs() << "\n\t  - IN_TEMP:\t\t[ ";
            for (StringRef *curr_var = tempBlock->in_set.begin(); curr_var != tempBlock->in_set.end(); ++curr_var) {
                errs() << curr_var->str()  << ", ";
            }
            errs() << "]";
            errs() << "\n\t\t\t\t ==" << setsEqual(&currBlock->in_set, &tempBlock->in_set);
            errs() << "\n\t  - OUT:\t\t[ ";
            for (StringRef *curr_var = currBlock->out_set.begin(); curr_var != currBlock->out_set.end(); ++curr_var) {
                errs() << curr_var->str()  << ", ";
            }
            errs() << "]";
            errs() << "\n\t  - OUT_TEMP:\t\t[ ";
            for (StringRef *curr_var = tempBlock->out_set.begin(); curr_var != tempBlock->out_set.end(); ++curr_var) {
                errs() << curr_var->str()  << ", ";
            }
            errs() << "]";
            errs() << "\n\t\t\t\t ==" << setsEqual(&currBlock->out_set, &tempBlock->out_set);
        }

        void printSet(SmallVector<StringRef, 64> *set, StringRef label) {
            errs() << "\n" << label << "[ ";
            for (StringRef elem: *set) {
                errs() << elem.str() << ", ";
            }
            errs() << " ]";
        }
    };
    
}
char SimpleDCE::ID = 0;
static RegisterPass<SimpleDCE> X("skeletonpass", "Simple dead code elimination");