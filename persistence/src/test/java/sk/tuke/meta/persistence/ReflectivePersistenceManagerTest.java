package sk.tuke.meta.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sk.tuke.meta.example.Department;
import sk.tuke.meta.example.Person;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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

    private void assertSqlHasResult(String sql) throws SQLException {
        var statement = connection.prepareStatement(sql);
        assertTrue(statement.executeQuery().next());
    }
}
