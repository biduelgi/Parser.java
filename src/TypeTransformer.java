public class TypeTransformer {
    public static Program T(Program p, TypeMap tm){
        Block body = (Block) T(p.body, tm);
        return new Program(p.decpart, body);
    }

    public static Statement T(Statement s, TypeMap tm){
        if(s instanceof Skip) return s;
        if(s instanceof Assignment){
            Assignment a = (Assignment) s;
            Variable target = a.target;
            Expression src = T(a.source, tm);
            Type ttype = (Type) tm.get(a.target);
            Type srctype = TypeChecker.typeOf(a.source, tm);
            if(ttype==Type.FLOAT){
                if(srctype==Type.INT){
                    src = new Unary(new Operator(Operator.I2F), src);
                    srctype = Type.FLOAT;
                }
            }
            else if(ttype == Type.INT){
                if(srctype==Type.CHAR){
                    src = new Unary(new Operator(Operator.C2I), src);
                    srctype = Type.INT;
                }
            }
            TypeChecker.check(ttype==srctype, "not equal type: + target");
            return new Assignment(target, src);
        }
        if(s instanceof Conditional){
            Conditional c = (Conditional) s;
            Expression test = T(c.test, tm);
            Statement tbr = T(c.thenbranch, tm);
            Statement ebr = T(c.elsebranch, tm);
            return new Conditional(test, tbr, ebr);
        }
        if(s instanceof Loop){
            Loop l=(Loop) s;
            Expression test = T(l.test, tm);
            Statement body = T(l.body, tm);
            return new Loop(test, body);
        }
        if(s instanceof Block){
            Block b = (Block) s;
            Block b2 = new Block();
            for(Statement sm: b.members) b2.members.add(T(sm, tm));
            return b2;
        }
        if(s instanceof Print){
            Print p = (Print) s;
            Expression exp = T(p.exp, tm);
            return new Print(exp);
        }
        throw new IllegalArgumentException("should never reach here");
    }
    public static Expression T(Expression e, TypeMap tm){
        if(e instanceof Value) return e;
        if(e instanceof Variable) return e;
        if(e instanceof Binary){
            Binary b = (Binary) e;
            Type typ1 = TypeChecker.typeOf(b.term1, tm);
            Expression e1 = T(b.term1, tm);
            Expression e2 = T(b.term2, tm);
            if(typ1==Type.INT) return new Binary(Operator.intMap(b.op.val), e1, e2);
            if(typ1==Type.FLOAT) return new Binary(Operator.floatMap(b.op.val), e1, e2);
            if(typ1==Type.CHAR) return new Binary(Operator.charMap(b.op.val), e1, e2);
            if(typ1==Type.BOOL) return new Binary(Operator.boolMap(b.op.val), e1, e2);
        }
        if(e instanceof Unary){
            Unary u = (Unary) e;
            Type type = TypeChecker.typeOf(u.term, tm);
            Expression e1 = T(u.term, tm);
            if(type==Type.BOOL&&u.op.NotOp()) return new Unary(Operator.boolMap(u.op.val), e1);
            if(type==Type.INT&&u.op.NegateOp()) return new Unary(Operator.intMap(u.op.val), e1);
            if(type==Type.FLOAT&&u.op.NegateOp()) return new Unary(Operator.floatMap(u.op.val), e1);
            if(type==Type.CHAR&&u.op.intOp()) return new Unary(Operator.charMap(u.op.val), e1);
            if(type==Type.FLOAT&&u.op.intOp()) return new Unary(Operator.floatMap(u.op.val), e1);
            if(type==Type.INT&&(u.op.floatOp()||u.op.charOp())) return new Unary(Operator.intMap(u.op.val), e1);
        }
        throw new IllegalArgumentException("should never reach here");
    }
    public static void main(String[] args) {
        Parser parser  = new Parser(new Lexer("p2.cl"));
        Program prog = parser.program();
        prog.display();
        System.out.println("Type map:");
        TypeMap map = TypeChecker.typing(prog.decpart);
        map.display();
        TypeChecker.V(prog);
        Program out = T(prog, map);
        System.out.println("Output AST");
        out.display();
    } //main
}
