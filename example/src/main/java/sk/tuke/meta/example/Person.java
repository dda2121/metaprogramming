package sk.tuke.meta.example;

import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;

@Table
public class Person {
    @Id
    @Column
    private long id;
    @Column
    private String surname;
    @Column
    private String name;
    @Column
    private int age;
    @Column(lazyFetch = true, targetClass = Department.class)
    private IDepartment department;

    public Person() {
    }

    public Person(String surname, String name, int age) {
        this.surname = surname;
        this.name = name;
        this.age = age;
    }

    public long getId() {
        return id;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setDepartment(IDepartment department) {
        this.department = department;
    }

    public IDepartment getDepartment() {
        return department;
    }

    @Override
    public String toString() {
        return String.format("Person %d: %s %s (%d)", id, surname, name, age);
    }
}