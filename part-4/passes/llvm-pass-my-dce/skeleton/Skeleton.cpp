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
            SmallVector<Value*, 64> in_set;
            SmallVector<Value*, 64> out_set;
        };

        SmallVector<Instruction*, 64> Worklist;          // Instructions to remove list.
        SmallVector<LivenessBlock, 64> lastLivePass;     // Stores temp state of all Instructions IN/OUT sets.
        SmallVector<LivenessBlock, 64> instLiveness;     // List of Instruction Liveness data.
        
        string removed;                             // Removed Instructions.
        
        static char ID;
        SimpleDCE() : FunctionPass(ID) {}
        virtual bool runOnFunction(Function &F) {
            // Output Colours
            char blue[] = { 0x1b, '[', '1', ';', '3', '4', 'm', 0 };
            char red[] = { 0x1b, '[', '1', ';', '3', '1', 'm', 0 };
            char mag[] = { 0x1b, '[', '1', ';', '3', '5', 'm', 0 };
            char normal[] = { 0x1b, '[', '0', ';', '3', '9', 'm', 0 };
            char grey[] = { 0x1b, '[', '0', ';', '3', '6', 'm', 0 };

            llvm::raw_string_ostream summary(removed);

            errs() << grey; printHeader("Starting Function Pass"); errs() << normal;

            bool instrRemoved;
            // // Do Liveness Analysis on entire Program and remove instructions.
            do {
                instrRemoved = false;

                // Generate List of instLiveness of all Instructions.
                for (auto bb = F.getBasicBlockList().rbegin(), e = F.getBasicBlockList().rend(); bb != e; ++bb) {
                    for (BasicBlock::reverse_iterator i = bb->rbegin(), e = bb->rend(); i != e; ++i) {
                        // Declare all members.
                        Instruction *currInst= &*i;
                        SmallVector<Value*, 64> currInst_in_set;
                        SmallVector<Value*, 64> currInst_out_set;

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
                errs() << mag; printHeader("Calculating Liveness"); errs() << normal;
                do {
                    livenessCalculating = false;

                    // Compute the IN/OUT sets for all Instructions in all BasicBlocks.
                    for (auto bb = F.getBasicBlockList().rbegin(), e = F.getBasicBlockList().rend(); bb != e; ++bb) {
                        errs() << mag; printSubHeader(bb->getName()); errs() << normal;
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
                errs() << mag; printFooter(); errs() << normal;
                

                // Loop through all Instructions in the Program, and mark dead Instructions for deletion.
                errs() << blue; printHeader("Checking Instructions Liveness"); errs() << normal;
                for (auto bb = F.getBasicBlockList().rbegin(), e = F.getBasicBlockList().rend(); bb != e; ++bb) {
                    // Loop through all Instructions in the current BasicBlock.
                    for (BasicBlock::reverse_iterator i = bb->rbegin(), e = bb->rend(); i != e; ++i) {
                        // Get Data required.
                        Instruction *currInst = &*i;
                        SmallVector<Value*, 64> currInstDEF = getDef(currInst, 0);
                        SmallVector<Value*, 64> currInstOUT = getInstLiveness(currInst).out_set;

                        // If this Instruction does not have a DEF, skip it.
                        if (currInstDEF.size() == 0) continue;
                        Value *instDef = currInstDEF[0];
                        
                        // If this Instructions DEF is not in its OUT set, mark it for removal.
                        if (!setContains(instDef, &currInstOUT)) {
                            Worklist.push_back(currInst);
                            errs() << mag;
                        }
                        errs() << "\n\t";
                        currInst->print(errs());
                        errs() << normal;
                    }
                }
                errs() << blue; printFooter(); errs() << normal;
                
                // Remove Dead Instructions.
                errs() << red; printHeader("Removing Insts"); errs() << normal;
                for (Instruction *i: Worklist) {
                    errs() << "\n\t";
                    if (safeToRemove(i)) {
                        errs() << red;
                        i->print(errs());
                        errs() << normal;
                        instrRemoved = true;
                        summary << "\n\t"; i->print(summary);       // Add to Summary.
                        i->eraseFromParent();
                    }
                    else {
                        errs() << normal;
                        i->print(errs());
                        errs() << normal;
                    }
                }
                Worklist.clear();
                lastLivePass.clear();
                instLiveness.clear();
                
                errs() << red; printFooter(); errs() << normal;
            } while (instrRemoved);

            errs() << red << "\n\n\n\n"; printSubHeader("Removed Insts Summary");
            errs() << removed;
            errs() << red; printSubFooter(); errs() << normal;
            errs() << grey; printFooter(); errs() << normal << "\n";
            return false;
        }

        void calculateInOutSets(BasicBlock *bb) {
            char normal[] = { 0x1b, '[', '0', ';', '3', '9', 'm', 0 };

            // Loop through Instructions in reverse, calculating IN/OUT sets until convergence.
            bool changed;
            for (BasicBlock::reverse_iterator i = bb->rbegin(), e = bb->rend(); i != e; ++i) {
                // Get the current Instruction.
                Instruction *currInst= &*i;

                errs() << "\n\t";
                currInst->print(errs());
                errs() << normal;

                // Get USE/DEF
                SmallVector<Value*, 64> use_set = getUse(currInst, 1);
                SmallVector<Value*, 64> def_set = getDef(currInst, 1);
            
                // Store the current instructions IN and OUT sets.
                SmallVector<Value*, 64> in_temp   = getInstLiveness(currInst).in_set;
                SmallVector<Value*, 64> out_temp  = getInstLiveness(currInst).out_set;

                // Store to global temp structure.
                lastLivePass[getTempInstLiveIdx(currInst)].in_set  = in_temp;
                lastLivePass[getTempInstLiveIdx(currInst)].out_set = out_temp;

                // Calculate new IN/OUT sets.
                SmallVector<Value*, 64> new_in;
                SmallVector<Value*, 64> new_out;

                // If this is a BranchInst, can have multiple successors.
                if (isa<BranchInst>(currInst)) {
                    // Do something with all successors.
                    // out[n] = UNION in[n+1]
                    for (int j=0; j < cast<BranchInst>(currInst)->getNumSuccessors(); j++) {
                        BasicBlock *branchBlock = cast<BranchInst>(currInst)->getSuccessor(j);
                        LivenessBlock branchInst = getInstLiveness(&branchBlock->front());
                        for (Value *inSetVar: branchInst.in_set) {
                            if (!setContains(inSetVar, &new_out)) new_out.push_back(inSetVar);
                        }
                    }
                    // in[n] = use[n]
                    new_in  = use_set;
                    // in[n] += (out[n] - def[n]) [EXCLUDING Duplicates]
                    for (Value *currVar: new_out) {
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
                    
                    new_out = getInstLiveness(nextInst).in_set;     // out[n] = UNION of in[n+1]
                    new_in  = use_set;                              // in[n] = use[n]
                    for (Value *currVar: new_out) {           // in[n] += (out[n] - def[n]) [EXCLUDING Duplicates]
                        if (!setContains(currVar, &def_set) && !setContains(currVar, &new_in)) {
                            new_in.push_back(currVar);
                        }
                    }
                }

                // Update our Liveness model.
                instLiveness[getInstLivenessIdx(currInst)].in_set  = new_in;
                instLiveness[getInstLivenessIdx(currInst)].out_set = new_out;
            }
        }

        bool safeToRemove(Instruction *i) {
            // Output Colours
            char blue[] = { 0x1b, '[', '1', ';', '3', '4', 'm', 0 };
            char red[] = { 0x1b, '[', '1', ';', '3', '1', 'm', 0 };
            char mag[] = { 0x1b, '[', '1', ';', '3', '5', 'm', 0 };
            char green[] = { 0x1b, '[', '1', ';', '3', '3', 'm', 0 };
            char normal[] = { 0x1b, '[', '0', ';', '3', '9', 'm', 0 };

            if (i->isTerminator()) { errs() << green << "isTerminator\t\t"; return false; }
            if (i->mayHaveSideEffects()) { errs() << green<< "mayHaveSideEffects\t"; return false; }
            if (isa<ReturnInst>(i) || isa<SwitchInst>(i) || isa<BranchInst>(i) || isa<IndirectBrInst>(i) || isa<CallInst>(i)) { errs() << green<< "isa<OTHER>(i)\t\t";  return false; }
            if (isa<StoreInst>(i)) { errs() << green<< "isa<StoreInst>(i)\t\t";  return false; }
            errs() << red << "    ----->\t\t" << normal;
            if (isa<AllocaInst>(i)) return true;
            if (isa<LoadInst>(i)) return true;
            if (isa<PHINode>(i)) return true;
            if (isa<GetElementPtrInst>(i)) return true;
            if (isa<SelectInst>(i)) return true;
            if (isa<ExtractElementInst>(i)) return true;
            if (isa<InsertElementInst>(i)) return true;
            if (isa<ExtractValueInst>(i)) return true;
            if (isa<InsertValueInst>(i)) return true;
            if (isa<BinaryOperator>(i)) return true;
            if (isa<ICmpInst>(i)) return true;
            if (isa<FCmpInst>(i)) return true;
            if (isa<TruncInst>(i)) return true;
            if (isa<ZExtInst>(i)) return true;
            if (isa<SExtInst>(i)) return true;
            if (isa<FPToUIInst>(i)) return true;
            if (isa<FPToSIInst>(i)) return true;
            if (isa<UIToFPInst>(i)) return true;
            if (isa<SIToFPInst>(i)) return true;
            if (isa<FPTruncInst>(i)) return true;
            if (isa<FPExtInst>(i)) return true;
            if (isa<PtrToIntInst>(i)) return true;
            if (isa<IntToPtrInst>(i)) return true;
            if (isa<BitCastInst>(i)) return true;
            if (isa<AddrSpaceCastInst>(i)) return true;
            return false;
        }

        SmallVector<Value*, 64> getUse(Instruction *i, int print_out) {
            // Get the USE of this instruction.
            SmallVector<Value*, 64> output;
            for (Use &U : i->operands()) {
                Value *v = U.get();
                output.push_back(v);
            }
            return output;
        }

        SmallVector<Value*, 64> getDef(Instruction *i, int print_out) {
            // Get the DEF of this instruction.
            SmallVector<Value*, 64> output;
            output.push_back(i);
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

        // Returns if two Sets are equal/permutations of one another.
        bool setsEqual(SmallVector<Value*, 64> *set_a, SmallVector<Value*, 64> *set_b) {
            return std::is_permutation(set_a->begin(), set_a->end(), set_b->begin());
        }

        // Returns if a Value pointer (i.e. a variable) exists in a Set.
        bool setContains(Value *target, SmallVector<Value*, 64> *set) {
            bool output = false;
            for (Value *elem: *set) {
                if (elem == target) output = true;
            }
            return output;
        }

        // -- HELPERS

        void printHeader(string heading) {
            int line_len = 44;
            int num_dash = line_len - heading.length();
            if (num_dash % 2 != 0) num_dash++;
            string output = "\n// ";
            for (int i=0; i < (num_dash/2); i++) {
                output += "-";
            }
            output += " " + heading + " ";
            for (int i=0; i < (num_dash/2); i++) {
                output += "-";
            }
            errs() << output << " \\\\\n";
        }

        void printFooter() {
            errs() << "\n\n\\\\ ---------------------------------------------- //";
        }

        void printSubHeader(string heading) {
            int line_len = 34;
            int num_dash = line_len - heading.length();
            if (num_dash % 2 != 0) num_dash++;
            string output = "\n\n    | ";
            for (int i=0; i < (num_dash/2); i++) {
                output += "-";
            }
            output += "[ " + heading + " ]";
            for (int i=0; i < (num_dash/2); i++) {
                output += "-";
            }
            errs() << output << " |\n";
        }

        void printSubFooter() {
            errs() << "\n\n    | --------------------------------------- |\n";
        }
    };
    
}
char SimpleDCE::ID = 0;
static RegisterPass<SimpleDCE> X("skeletonpass", "Simple dead code elimination");