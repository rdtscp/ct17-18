#define DEBUG_TYPE "skeletonpass"
#include "llvm/Pass.h"
#include "llvm/IR/BasicBlock.h"
#include "llvm//Transforms/Utils/Local.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/Instruction.h"
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
        SmallVector<Instruction*, 64> Worklist;
        static char ID;
        SimpleDCE() : FunctionPass(ID) {}
        virtual bool runOnFunction(Function &F) {
            // Output Colours
            char blue[] = { 0x1b, '[', '1', ';', '3', '4', 'm', 0 };
            char normal[] = { 0x1b, '[', '0', ';', '3', '9', 'm', 0 };
            
            errs() << "\nFunction " << F.getName() << '\n';

            SmallVector<StringRef, 64> phiVars;

            bool removing;
            do {
                removing = false;
                // Find trivially dead instructions.
                // auto bblist = F.getBasicBlockList();
                for (auto bb = F.getBasicBlockList().rbegin(), e = F.getBasicBlockList().rend(); bb != e; ++bb) {
                    errs() << "\n\n ----> Inside Block: " << bb->getName();

                    // Create IN and OUT sets for this BasicBlock's instructions.
                    // n'th element = instruction n's set.
                    SmallVector<SmallVector<StringRef, 64>, 64> in_set;
                    SmallVector<SmallVector<StringRef, 64>, 64> out_set;

                    assert(in_set.size() == 0);
                    assert(out_set.size() == 0);

                    // Loop through the instructions initializing the IN and OUT sets.
                    for (BasicBlock::reverse_iterator i = bb->rbegin(), e = bb->rend(); i != e; ++i) {
                        SmallVector<StringRef, 64> curr_inst_in_set;
                        SmallVector<StringRef, 64> curr_inst_out_set;
                        in_set.push_back(curr_inst_in_set);
                        out_set.push_back(curr_inst_out_set);
                    }
                    
                    // Loop through instructions, 
                    bool changed;
                    do {
                        changed = false;
                        int inst_num = bb->size() - 1;
                        for (BasicBlock::reverse_iterator i = bb->rbegin(), e = bb->rend(); i != e; ++i, inst_num--) {
                            errs() << "\n\nInst-" << inst_num;
                            // Get the current Instruction.
                            Instruction *currInst= &*i;

                            // Handle PHI.
                            if (isa<PHINode>(currInst)) {
                                for (Use &U: currInst->operands()) {
                                    Value *v = U.get();
                                    if (v->getName() != "") {
                                        phiVars.push_back(v->getName());
                                    }
                                }
                            }

                            SmallVector<StringRef, 64> use_set;
                            SmallVector<StringRef, 64> def_set;
                            assert(use_set.size() == 0);
                            assert(def_set.size() == 0);
                            
                            // Get the USE of this instruction.
                            errs() << "\n\tUSE:";                        
                            for (Use &U : currInst->operands()) {
                                Value *v = U.get();
                                if (v->getName() != "") {
                                    errs() << "\n\t" << v->getName();
                                    use_set.push_back(v->getName());
                                }
                            }
                            // Get the DEF of this instruction.
                            if (currInst->getName() != "") {
                                errs() << "\n\tDEF:";                            
                                errs() << "\n\t" << currInst->getName();
                                def_set.push_back(currInst->getName());
                            }

                            // Store the current instructions IN and OUT sets.
                            SmallVector<StringRef, 64> in_temp   = in_set[inst_num];
                            SmallVector<StringRef, 64> out_temp  = out_set[inst_num];
                            

                            // If we are at last instruction in the BB; i.e. no successors.
                            if (inst_num == (bb->size() - 1)) {
                                // Clear the OUT set.
                                out_set[inst_num].clear();
                                // IN set = UNION[USE, (OUT - DEF)]
                                in_set[inst_num].clear();
                                // Push all USE elements to IN set (given they don't already exist).
                                for (StringRef *var = use_set.begin(); var != use_set.end(); ++var) {
                                    if (!setContains(*var, &in_set[inst_num]))
                                        in_set[inst_num].push_back(*var);
                                }
                                // OUT Set is Empty - No work to do.
                            }
                            // If we are not the last instruction; since we are in BB, can only have 1 successor.
                            else {
                                // Clear the OUT set.
                                out_set[inst_num].clear();
                                // Add the variables in the IN set for next instruction (given they don't already exist).
                                for (StringRef *var = in_set[inst_num + 1].begin(); var != in_set[inst_num + 1].end(); ++var) {
                                    if (!setContains(*var, &out_set[inst_num]))
                                        out_set[inst_num].push_back(*var);
                                }

                                // Push all USE elements to IN set (given they don't already exist).
                                for (StringRef *var = use_set.begin(); var != use_set.end(); ++var) {
                                    if (!setContains(*var, &in_set[inst_num]))
                                        in_set[inst_num].push_back(*var);
                                }

                                // Push the (OUT - DEF) set elements to the IN set.
                                for (StringRef *var = out_set[inst_num].begin(); var != out_set[inst_num].end(); ++var) {
                                    if (!setContains(*var, &def_set) && !setContains(*var, &in_set[inst_num]))
                                        in_set[inst_num].push_back(*var);
                                }

                            }


                            // Print the SET States.
                            errs() << "\n\t  - IN:\t\t\t[ ";
                            for (StringRef *curr_var = in_set[inst_num].begin(); curr_var != in_set[inst_num].end(); ++curr_var) {
                                errs() << curr_var->str()  << ", ";
                            }
                            errs() << "]";
                            errs() << "\n\t  - IN_TEMP:\t\t[ ";
                            for (StringRef *curr_var = in_temp.begin(); curr_var != in_temp.end(); ++curr_var) {
                                errs() << curr_var->str()  << ", ";
                            }
                            errs() << "]";

                            errs() << "\n\t\t\t\t ==" << compareSets(&in_set[inst_num], &in_temp);
                            


                            errs() << "\n\t  - OUT:\t\t[ ";
                            for (StringRef *curr_var = out_set[inst_num].begin(); curr_var != out_set[inst_num].end(); ++curr_var) {
                                errs() << curr_var->str()  << ", ";
                            }
                            errs() << "]";
                            errs() << "\n\t  - OUT_TEMP:\t\t[ ";
                            for (StringRef *curr_var = out_temp.begin(); curr_var != out_temp.end(); ++curr_var) {
                                errs() << curr_var->str()  << ", ";
                            }
                            errs() << "]";

                            errs() << "\n\t\t\t\t ==" << compareSets(&out_set[inst_num], &out_temp);
                            if ((!compareSets(&out_set[inst_num], &out_temp)) || !(compareSets(&in_set[inst_num], &in_temp))) changed = true;
                            use_set.clear();
                            def_set.clear();
                        }
                    } while(changed);
                    printSets(&in_set, &out_set);
                    
                    // Remove dead instructions.
                    int inst_num = 0;
                    for (BasicBlock::iterator i = bb->begin(), e = bb->end(); i != e; ++i, inst_num++) {
                        Instruction *currInst= &*i;
                        if (!setContains(currInst->getName().str(), &out_set[inst_num])) {
                            Worklist.push_back(currInst);
                        }
                    }
                    in_set.clear();
                    out_set.clear();
                }
                errs() << "\n//------------- Removing [" << Worklist.size() << "] Instructions ---------------\\\\";
                for (int i=(Worklist.size() - 1); i >= 0; i--) {
                    Instruction *currInst = Worklist[i];
                    if (isa<PHINode>(currInst)) {
                        phiVars = updatePHIVars(currInst, &phiVars);
                    }
                    if (safeToRemove(currInst, &phiVars)) {
                        errs() << "\n" << blue;
                        currInst->print(errs());
                        errs() << normal;
                        currInst->eraseFromParent();
                        removing = true;
                    }
                    else {
                        errs() << "\n";
                        currInst->print(errs());
                    }
                }
                Worklist.clear();
                errs() << "\n\\\\---------------------------------------------------//\n";
            } while (removing);
            return false;
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
            if (i->isTerminator()) return false;
            if (i->mayHaveSideEffects()) return false;
            if (isa<ReturnInst>(i) || isa<SwitchInst>(i) || isa<BranchInst>(i) || isa<IndirectBrInst>(i) || isa<CallInst>(i)) return false;
            if (isa<StoreInst>(i)) return false;
            // if (isa<LoadInst>(i)) return false;
            // if (isa<PHINode>(i)) return false;
            // Don't remove instructions that store to a variable used in a PHI.
            for (StringRef *phiVar = phiVars->begin(); phiVar != phiVars->end(); ++phiVar) {
                if (i->getName() == phiVar->str()) return false;
            }
            return true;
        }

        bool compareSets(SmallVector<StringRef, 64> *set_a, SmallVector<StringRef, 64> *set_b) {
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

        void unitTest() {
            SmallVector<StringRef, 64> test1;
            SmallVector<StringRef, 64> test2;
            test1.push_back("a");
            test1.push_back("b");
            test1.push_back("c");

            // test2.push_back("c");
            test2.push_back("b");
            test2.push_back("a");

            errs() << "\n\t\tcompareSets(test1, test2) :: " << compareSets(&test1, &test2);
            
        }

        void printSets(SmallVector<SmallVector<StringRef, 64>, 64> *in_set, SmallVector<SmallVector<StringRef, 64>, 64> *out_set) {
            errs() << "\n----------------------";
            // Loop through all Instructions.
            for (int i=0; i < in_set->size(); i++) {
                errs() << "\n\t Inst " << i << ":";
                SmallVector<StringRef, 64> curr_inst_in = (*in_set)[i];
                SmallVector<StringRef, 64> curr_inst_out = (*out_set)[i];
                errs() << "\n\t  - IN:\t\t[ ";
                for (StringRef *curr_var = curr_inst_in.begin(); curr_var != curr_inst_in.end(); ++curr_var) {
                    errs() << curr_var->str()  << ", ";
                }
                errs() << "]";
                errs() << "\n\t  - OUT:\t[ ";
                for (StringRef *curr_var = curr_inst_out.begin(); curr_var != curr_inst_out.end(); ++curr_var) {
                    errs() << curr_var->str()  << ", ";
                }
                errs() << "]";
            }
            errs() << "\n----------------------";
        }
    };
    
}
char SimpleDCE::ID = 0;
static RegisterPass<SimpleDCE> X("skeletonpass", "Simple dead code elimination");