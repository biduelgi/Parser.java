public class TypeChecker {

    public static TypeMap typing(Declarations d){
        TypeMap map = new TypeMap();
        for(Declaration di:d){
            map.put(di.v, di.t);
        }
        return map;
    }

    public static void check(boolean test, String msg){
        if(test) return;
        System.err.println(msg);
        System.exit(1);
    }

    public static void V(Declarations d){
        for(int i=0; i<d.size()-1; i++){
            for(int j=i+1; j<d.size();j++){
                Declaration di = d.get(i);
                Declaration dj = d.get(j);
                check(!(di.v.equals(dj.v)), "duplicate declaration: " + dj.v);
            }
        }
    }

    public static void V(Program p){
        V(p.decpart);
        V(p.body, typing(p.decpart));
    }

    public static void V(Statement s, TypeMap tm){
        if(s==null) throw new IllegalArgumentException("AST error: null stmt");
        if(s instanceof Skip) return;
        if(s instanceof Assignment){
            Assignment a = (Assignment) s;
            check(tm.containsKey(a.target), "undefined target in assignment: " + a.target);
            V(a.source, tm);
            Type ttype = tm.get(a.target);
            Type srctype = typeOf(a.source, tm);
            if(ttype!=srctype){
                if(ttype==Type.FLOAT) check(srctype==Type.INT, "type error: " + a.target);
                else if(ttype==Type.INT) check(srctype==Type.CHAR, "type error: " + a.target);
                else check(false, "mixed mode assignment to " + a.target);
            }
            return;
        }
        if(s instanceof Conditional){
            Conditional c = (Conditional) s;
            V(c.test, tm);
            Type ttype = typeOf(c.test, tm);
            if(ttype==Type.BOOL){
                V(c.thenbranch, tm);
                V(c.elsebranch, tm);
                return;
            }
            else check(false, "must be bool type in if condition: "+c.test);
        }
        if(s instanceof Loop){
            Loop l = (Loop) s;
            V(l.test, tm);
            Type ttype = typeOf(l.test, tm);
            if(ttype == Type.BOOL){
                V(l.body, tm);
                return;
            }
            else check(false, "must be bool type in while condition: "+l.test);
        }
        if(s instanceof Block){
            Block b = (Block) s;
            for(Statement sm:b.members) V(sm, tm);
        }
    }

    public static void V(Expression e, TypeMap tm){
        if(e instanceof Value) return;
        if(e instanceof Variable){
            Variable v1 = (Variable) e;
            check(tm.containsKey(v1), "undeclared variable: "+v1);
        }
        if(e instanceof Binary){
            Binary b = (Binary) e;
            Type typ1 = typeOf(b.term1, tm);
            Type typ2 = typeOf(b.term2, tm);
            V(b.term1, tm);
            V(b.term2, tm);
            if(b.op.ArithmeticOp()) check(typ1==typ2&&(typ1==Type.INT||typ1==Type.FLOAT),
                    "type error in arithmetic op: " + b.op);
            else if(b.op.RelationalOp()) check(typ1==typ2,
                    "type error in relational op: "+b.op);
            else if(b.op.BooleanOp()) check(typ1==typ2&&typ1==Type.BOOL,
                    "type error in boolean op: "+b.op);
            else throw new IllegalArgumentException("type error in Binary op: "+b.op);
            return;
        }
        if(e instanceof Unary){
            Unary u = (Unary) e;
            Type type = typeOf(u.term, tm);
            V(u.term, tm);
            if(u.op.NotOp()) check(type==Type.BOOL, "type error in Not op"+u.op);
            else if (u.op.NegateOp()) check(type==Type.INT||type==Type.FLOAT, "type error in Negate op"+u.op);
            else if(u.op.intOp()) check(type==Type.CHAR||type==Type.FLOAT, "type error in int op"+u.op);
            else if(u.op.floatOp()) check(type==Type.INT, "type error in float op"+u.op);
            else if(u.op.charOp()) check(type==Type.INT, "type error in char op"+u.op);
            else throw new IllegalArgumentException("type error in Unary op: "+u.op);
        }
    }

    public static Type typeOf(Expression e, TypeMap tm){
        if(e instanceof Value) return ((Value)e).type;
        if(e instanceof Variable){
            Variable v =(Variable) e;
            return tm.get(v);
        }
        if(e instanceof Binary){
            Binary b = (Binary) e;
            if(b.op.ArithmeticOp()) return typeOf(b.term1, tm);
            else if(b.op.RelationalOp()||b.op.BooleanOp()) return(Type.BOOL);
        }
        if(e instanceof Unary){
            Unary u = (Unary) e;
            if(u.op.NotOp()) return(Type.BOOL);
            else if(u.op.NegateOp()) return typeOf(u.term, tm);
            else if(u.op.intOp()) return(Type.INT);
            else if(u.op.floatOp()) return(Type.FLOAT);
            else if(u.op.charOp()) return(Type.CHAR);
        }
        throw new IllegalArgumentException("should never reach here");
    }
    public static void main(String[] args){
        Parser parser = new Parser(new Lexer("p2.cl"));
        Program prog = parser.program();
        TypeMap map = typing(prog.decpart);
        V(prog);
        System.out.println("There is no type error");
        System.out.print("Type map: ");
        map.display();
        prog.display();
    }
}
