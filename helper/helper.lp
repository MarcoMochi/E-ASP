#script (python)

import sys
import clingo
import re

atom_to_symbol = {}
choices_lit = []
order = []
heads = []
final_order = []

def finder(value, elements):
    for x in elements:
        if x == value:
            return True
    return False

class Propagator:
    def init(self, init):
        for atom in init.symbolic_atoms:
            heads.append(str(atom.symbol))
            if atom.literal not in choices_lit:
                continue
            if not init.solver_literal(atom.literal) in atom_to_symbol.keys():
                atom_to_symbol[init.solver_literal(atom.literal)] = []
            atom_to_symbol[init.solver_literal(atom.literal)].append(atom.symbol)
            init.add_watch(init.solver_literal(atom.literal))
            if init.assignment.is_fixed(init.solver_literal(atom.literal)) and init.assignment.is_true(init.solver_literal(atom.literal)):
                order.append(atom.__str__())
        
            
        

    def propagate(self, ctl, changes):
        for x in changes:
            for y in atom_to_symbol[x]:
                temp_str = "not " + y.__str__()
                if temp_str in order:
                    order.remove(temp_str)
                order.append(y.__str__())
    

    def decide(self, thread_id, assignment, fallback):
        try:
            l = re.search("\d+", str(fallback)).group(0)
            l = int(l)
            if l in atom_to_symbol.keys():
                for atom in atom_to_symbol[l]:
                    indeces = []
                    elem = atom.__str__()
                    if elem in order:
                        order.remove(elem)
                    order.append("not " + elem)
                    
                    
        except:
            pass
        return fallback

    def undo(self, thread_id, assignment, changes):
        for l in changes:
            for y in atom_to_symbol[l]:
                if finder(y.__str__(), order):
                    order.remove(y.__str__())
                    


def on_model(res):
    global final_order
    final_order.append(order.copy())


class Observer:
     def rule(self, choice, head, body):
        global heads
        for h in head:
            choices_lit.append(h)
    
def main(prg):
    prg.register_observer(Observer())
    prg.register_propagator(Propagator())
    prg.ground([("base", [])])
    with prg.solve(yield_=True, on_model=on_model) as hnd:
        for m in hnd:
            continue
        hnd.wait()
    with open("./.tmp_file2", "w") as w:
        w.write(f"start:{';'.join(heads)}")
        for line in final_order:
            w.write(f"\n-mid-{';'.join([x for x in line if 'clingo.symbolic_atoms' not in x])}")
#end.