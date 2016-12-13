
# Clojure Database Proposal

## Sections
------

1. Introduction
1. Queries
1. Use Cases

## Introduction

Somehow we are going create a database management system written in Clojure.

## Queries

Initially we will support two types of queries: insert and retrieval.

```
    { insert : <JSON-Document> }
    { locate : <JSON-Query> }
```

Examples

```
    { insert : { name : "Jeffrey" , age : 33 , location : { city : "Tuscaloosa" , state : "Alabama"  } } }

    { locate : { name : "Jeffrey" } }
    { locate : { location.city : "Tuscaloosa" } }
    { locate : { $or : { name : "Jeffrey" , location.city : "Tuscaloosa" } } }
```

## Use Cases

#### Insertion of documents

##### Insertion of an empty document

**Query**
```
    { insert : {} }
```

**Result**

```
    no documents inserted
```

#### Insertion of malformed document


**Query**
```
    { insert : {a} }
    { insert : {a : } }
```

**Result**

```
    Error: Malformed document
    Error: Malformed document
```

#### Insertion of a document with a single field and value


**Query**
```
    { insert : {character : "b" } }
    { insert : {age : 1 } }
    { insert : {document : {} } }
    { insert : {a : [] } }
```

**Result**

```
    document inserted:
    {
        character : "b"
    }
    document inserted:
    {
        age : 1
    }
    document inserted:
    {
        document : {}
    }
    document inserted:
    {
        listOfStuff : []
    }
```


#### Insertion of multi-field document

**Query**
```
    { insert : { firstName : "Jeff" , lastName: "Robinson" } }
    { insert : { firstName : "Jeff" , lastName: "Robinson" , age : 33 } }
    { insert : { id : 1 , date: "04 Dec 2016 12:00:00:00" , price : 12.00 , quantity : 100 , total : 1200 } }
```

**Result**

```
    document inserted:
    { 
        firstName : "Jeff",
        lastName  : "Robinson", 
    }
    document inserted:
    { 
        firstName : "Jeff",
        lastName  : "Robinson",
        age       : 33 
    }
    document inserted:
    { 
        id          : 1,
        date        : "04 Dec 2016 12:00:00:00",
        price       : 12.00,
        quantity    : 100,
        total       : 1200
    }
```

#### Insertion of nested documents


**Query**
```
    { insert : { A : { B : "Message" } } }
    { insert : { A : { B : { C : { D : "deepest!" } } } } }
    { insert : { A : { B : "Multiple Values" , C : { D : { E : "deepest!" } } } } }
    
```

**Result**

```
    document inserted:
    { 
        A : {
            B : "Message" 
        }
    }
    document inserted:
    { 
        A : { 
            B : { 
                C : { 
                    D : "deepest!" 
                    } 
                } 
            } 
        } 
    }
    document inserted:
    { 
        A : { 
            B : "Multiple Values", 
            C : { 
                D : { 
                    E : "deepest!" 
                } 
            } 
        } 
    }
```
