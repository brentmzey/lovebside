/**
 * Functional Programming Utilities for PocketBase (Scala-like Fluent API)
 * Provides Option, Try, and fluent collection helpers.
 */

// --- Option Monad ---
class Option {
    constructor(value) {
        this.value = value;
    }

    static of(value) {
        return (value === null || value === undefined) ? new None() : new Some(value);
    }

    static empty() {
        return new None();
    }

    // Abstract methods
    isEmpty() { throw new Error("Abstract method"); }
    isDefined() { throw new Error("Abstract method"); }
    get() { throw new Error("Abstract method"); }
    
    // Fluent API
    map(fn) { throw new Error("Abstract method"); }
    flatMap(fn) { throw new Error("Abstract method"); }
    filter(predicate) { throw new Error("Abstract method"); }
    getOrElse(defaultVal) { throw new Error("Abstract method"); }
    orElse(alternative) { throw new Error("Abstract method"); }
    foreach(fn) { throw new Error("Abstract method"); }
}

class Some extends Option {
    constructor(value) {
        super(value);
    }

    isEmpty() { return false; }
    isDefined() { return true; }
    get() { return this.value; }

    map(fn) {
        return Option.of(fn(this.value));
    }

    flatMap(fn) {
        const res = fn(this.value);
        return (res instanceof Option) ? res : Option.of(res);
    }

    filter(predicate) {
        return predicate(this.value) ? this : Option.empty();
    }

    getOrElse(defaultVal) {
        return this.value;
    }

    orElse(alternative) {
        return this;
    }

    foreach(fn) {
        fn(this.value);
    }
}

class None extends Option {
    constructor() {
        super(null);
    }

    isEmpty() { return true; }
    isDefined() { return false; }
    get() { throw new Error("NoSuchElementException: None.get"); }

    map(fn) { return this; }
    flatMap(fn) { return this; }
    filter(predicate) { return this; }
    
    getOrElse(defaultVal) {
        return (typeof defaultVal === 'function') ? defaultVal() : defaultVal;
    }

    orElse(alternative) {
        return (typeof alternative === 'function') ? alternative() : alternative;
    }

    foreach(fn) { /* no-op */ }
}

// --- Try Monad (Basic) ---
class Try {
    static apply(fn) {
        try {
            return new Success(fn());
        } catch (e) {
            return new Failure(e);
        }
    }
}

class Success {
    constructor(value) { this.value = value; }
    isSuccess() { return true; }
    isFailure() { return false; }
    get() { return this.value; }
    map(fn) { return Try.apply(() => fn(this.value)); }
    getOrElse(defaultVal) { return this.value; }
    toOption() { return Option.of(this.value); }
}

class Failure {
    constructor(exception) { this.exception = exception; }
    isSuccess() { return false; }
    isFailure() { return true; }
    get() { throw this.exception; }
    map(fn) { return this; }
    getOrElse(defaultVal) { return (typeof defaultVal === 'function') ? defaultVal() : defaultVal; }
    toOption() { return Option.empty(); }
}

// --- Schema Builder DSL ---
class SchemaBuilder {
    constructor(dao) {
        this.dao = dao;
    }

    static using(dao) {
        return new SchemaBuilder(dao);
    }

    ensureCollection(name, type = "base") {
        const existing = Option.of(this._findCollection(name));
        
        return {
            definedBy: (schemaDefinitionFn) => {
                const schema = schemaDefinitionFn(new CollectionDSL(name, type));
                
                existing.fold(
                    // If Empty (Create)
                    () => {
                        console.log(`✨ Creating ${name} collection`);
                        this.dao.save(schema.build());
                    },
                    // If Defined (Update/Ensure)
                    (col) => {
                        // Idempotent field addition logic could go here
                        // For now we just log existence
                        // console.log(`✅ ${name} exists`);
                    }
                );
            }
        };
    }

    _findCollection(name) {
        try {
            return this.dao.findCollectionByNameOrId(name);
        } catch (e) {
            return null;
        }
    }
}

class CollectionDSL {
    constructor(name, type) {
        this.config = {
            name: name,
            type: type,
            system: false,
            schema: [],
            indexes: []
        };
    }

    field(name, type, config = {}) {
        this.config.schema.push({
            name: name,
            type: type,
            ...config
        });
        return this;
    }

    relation(name, collectionId, config = {}) {
        return this.field(name, "relation", {
            collectionId: collectionId,
            cascadeDelete: false,
            maxSelect: 1,
            ...config
        });
    }

    index(def) {
        this.config.indexes.push(def);
        return this;
    }

    build() {
        return new Collection(this.config);
    }
}

// Attach to global for usage in migrations (Goja)
// In a module system we would export, but here we might need to copy-paste or load.
// Since we can't easily 'require' local files in migrations, we will embed this in the migration file.
