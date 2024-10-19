import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    public enum Kind {
        STATIC, FIELD, ARG, VAR
    }

    private Map<String, Symbol> classScopeTable;
    private Map<String, Symbol> subroutineScopeTable;
    private Map<Kind, Integer> indexCounters;

    public SymbolTable() {
        classScopeTable = new HashMap<>();
        subroutineScopeTable = new HashMap<>();
        indexCounters = new HashMap<>();
        for (Kind kind : Kind.values()) {
            indexCounters.put(kind, 0);
        }
    }

    public void reset() {
        subroutineScopeTable.clear();
        indexCounters.put(Kind.ARG, 0);
        indexCounters.put(Kind.VAR, 0);
    }


    public void define(String name, String type, Kind kind) {
        int index = indexCounters.get(kind);
        Symbol symbol = new Symbol(type, kind, index);

        if (kind == Kind.STATIC || kind == Kind.FIELD) {
            classScopeTable.put(name, symbol);
        } else {
            subroutineScopeTable.put(name, symbol);
        }

        indexCounters.put(kind, index + 1);
    }

    public int varCount(Kind kind) {
        return indexCounters.get(kind);
    }


    public Kind kindOf(String name) {
        Symbol symbol = lookup(name);
        return (symbol != null) ? symbol.kind : null;

    }

    public String typeOf(String name) {
        Symbol symbol = lookup(name);
        return (symbol != null) ? symbol.type : null;

    }

    public int indexOf(String name) {
        Symbol symbol = lookup(name);
        return (symbol != null) ? symbol.index : -1;

    }

    private Symbol lookup(String name) {
        if (subroutineScopeTable.containsKey(name)) {
            return subroutineScopeTable.get(name);
        } else if (classScopeTable.containsKey(name)) {
            return classScopeTable.get(name);
        } else {
            return null;
        }
    }

    private static class Symbol {
        String type;
        Kind kind;
        int index;

        Symbol(String type, Kind kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
    }
}