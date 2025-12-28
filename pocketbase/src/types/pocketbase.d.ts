// pocketbase.d.ts
declare namespace pb {
    interface Context {
        json(code: number, data: any): void;
        queryParam(key: string): string;
        formValue(key: string): string;
        bind(data: any): void;
        request(): {
            url: {
                query(): {
                    get(key: string): string;
                }
            }
        };
    }

    interface Logger {
        info(msg: string): void;
        warn(msg: string): void;
        error(msg: string): void;
    }

    interface Collection {
        id: string;
        name: string;
        type: string;
        createRule: string;
        listRule: string;
    }

    interface Dao {
        findCollectionByNameOrId(nameOrId: string): Collection;
        findAuthRecordByEmail(collection: string, email: string): Record;
        findCollections(): Collection[];
        saveRecord(record: Record): void;
        findRecordsByFilter(collection: string, filter: string, sort: string, limit: number, offset: number): Record[];
        // Add more as needed
    }

    interface Record {
        id: string;
        get(key: string): any;
        set(key: string, value: any): void;
        setUsername(username: string): void;
        setEmail(email: string): void;
        setPassword(password: string): void;
    }
}

declare const $app: {
    dao(): pb.Dao;
    logger(): pb.Logger;
};

declare const $apis: {
    requestInfo(c: pb.Context): { data: any, query: any };
};

declare var Record: {
    new(collection: any): pb.Record;
};

declare function routerAdd(method: string, path: string, handler: (c: pb.Context) => void): void;
