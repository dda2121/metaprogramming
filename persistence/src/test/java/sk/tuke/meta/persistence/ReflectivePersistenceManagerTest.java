package sk.tuke.meta.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sk.tuke.meta.example.Department;
import sk.tuke.meta.example.Person;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReflectivePersistenceManagerTest {

    private Connection connection;
    private ReflectivePersistenceManager manager;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        manager = new ReflectivePersistenceManager(connection);
        manager.createTables(Person.class, Department.class);
    }

    @Test
    void insertPersonWithoutDepartment() {
        Person person = new Person("Name", "Surname", 25);
        manager.save(person);
        assertNotEquals(0, person.getId());
    }

    @Test
    void insertDepartment() {
        Department department = new Department("ABC", "CDE");
        manager.save(department);
        assertNotEquals(0, department.getId());
    }

    @Test
    void insertPersonWithInsertedDepartment() {
        Department department = new Department("ABC", "CDE");
        manager.save(department);
        Person person = new Person("Name", "Surname", 25);
        person.setDepartment(department);
        manager.save(person);
        assertNotEquals(0, person.getId());
    }

    @Test
    void insertPersonWithNotInsertedDepartment() {
        Department department = new Department("ABC", "CDE");
        Person person = new Person("Name", "Surname", 25);
        person.setDepartment(department);
        manager.save(person);
        assertEquals(0, person.getId());
    }

    @Test
    void updateDepartment() throws SQLException {
        Department department = new Department("ABC", "CDE");
        manager.save(department);
        long id = department.getId();
        department.setCode("DFG");
        department.setName("KJH");
        manager.save(department);
        assertSqlHasResult("select * from department where id = " + id
                + " and name = 'KJH' and code = 'DFG'");
    }

    @Test
    void updatePersonWithSettingFieldToNull() throws SQLException {
        Department department = new Department("ABC", "CDE");
        manager.save(department);
        Person person = new Person("Surname", "Name", 25);
        person.setDepartment(department);
        manager.save(person);

        long personId = person.getId();
        long departmentId = department.getId();

        assertSqlHasResult("select * from person where id = " + personId
                + " and name = 'Name' and surname = 'Surname' and department = " + departmentId + " and age = 25");

        person.setDepartment(null);
        person.setName(null);
        person.setSurname(null);
        person.setAge(0);
        manager.save(person);

        assertSqlHasResult("select * from person where id = " + personId
                + " and name is null and surname is null and department is null and age = 0");
    }

    @Test
    void getExistingDepartment() {
        Department department = new Department("ABC", "CDE");
        manager.save(department);
        Optional<Department> d = manager.get(Department.class, department.getId());
        assertTrue(d.isPresent());
    }

    @Test
    void getNotExistingDepartment() {
        Optional<Department> d = manager.get(Department.class, 5);
        assertTrue(d.isEmpty());
    }

    @Test
    void getExistingPerson() {
        Person person = new Person("Name", null, 25);
        manager.save(person);
        Optional<Person> p = manager.get(Person.class, person.getId());
        assertTrue(p.isPresent());
    }

    @Test
    void getNotExistingPerson() {
        Optional<Person> p = manager.get(Person.class, 5);
        assertTrue(p.isEmpty());
    }

    @Test
    void getExistingPersonWithDepartment() {
        Department department = new Department("ABC", "CDE");
        manager.save(department);
        Person person = new Person("Name", "Surname", 25);
        person.setDepartment(department);
        manager.save(person);

        Optional<Person> p = manager.get(Person.class, person.getId());
        assertTrue(p.isPresent());
        assertNotNull(p.get().getDepartment());
        assertEquals(p.get().getDepartment().getId(), department.getId());
    }

    @Test
    void getAllZeroRaws() {
        List<Person> people = manager.getAll(Person.class);
        assertEquals(0, people.size());
    }

    @Test
    void getAllCountRaws() {
        Department department = new Department("ABC", "CDE");
        manager.save(department);
        Person person = new Person("Name", "Surname", 25);
        person.setDepartment(department);
        manager.save(person);
        Person person2 = new Person("Name1", "Surname1", 30);
        person2.setDepartment(department);
        manager.save(person2);

        List<Person> people = manager.getAll(Person.class);
        assertEquals(2, people.size());
    }

    void assertSqlHasResult(String sql) throws SQLException {
        var statement = connection.prepareStatement(sql);
        assertTrue(statement.executeQuery().next());
    }
}
