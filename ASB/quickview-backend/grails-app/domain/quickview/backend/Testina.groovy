package quickview.backend

import grails.persistence.*

@Entity
class Testina {

    String id
    String name
    Float  eight
    String gender

    static constraints = {
        name maxSize: 100
        eight nullable: true
        gender inList: ['male', 'female']
    }
}
