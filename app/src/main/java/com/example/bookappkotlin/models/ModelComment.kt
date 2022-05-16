package com.example.bookappkotlin.models

class ModelComment {

    //variables, should be with same spellings and type as in firebase
    var id = ""
    var bookId = ""
    var timestamp = ""
    var comment = ""
    var uid = ""

    //empty constructor, required by firebase
    constructor()


    //parameter constructor
    constructor(id: String, bookId: String, timestamp: String, comment: String, uid: String) {
        this.id = id
        this.bookId = bookId
        this.timestamp = timestamp
        this.comment = comment
        this.uid = uid
    }
}