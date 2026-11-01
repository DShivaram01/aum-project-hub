package com.aum.projecthub.demo;

import com.aum.projecthub.model.JoinRequest;
import com.aum.projecthub.model.Project;
import com.aum.projecthub.model.ProjectStatus;
import com.aum.projecthub.model.RequestStatus;
import com.aum.projecthub.model.Role;
import com.aum.projecthub.model.User;
import com.aum.projecthub.repository.JoinRequestRepository;
import com.aum.projecthub.repository.ProjectRepository;
import com.aum.projecthub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    public static final String PASSWORD = "demo1234";
    public static final String FACULTY_EMAIL = "maya.patel@demo.aum.edu";
    public static final String STUDENT_EMAIL = "alex.rivera@demo.aum.edu";

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataLoader(UserRepository userRepository, ProjectRepository projectRepository,
                          JoinRequestRepository joinRequestRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User maya = user("Dr. Maya Patel", FACULTY_EMAIL, Role.FACULTY, "Computer Science",
                "Java, Distributed Systems, Databases",
                "Associate professor. I run the systems lab and usually have room for two or three students.");
        User james = user("Dr. James Carter", "james.carter@demo.aum.edu", Role.FACULTY, "Electrical Engineering",
                "Embedded Systems, IoT, C",
                "Working on low-cost sensors for campus energy monitoring.");
        User alex = user("Alex Rivera", STUDENT_EMAIL, Role.STUDENT, "Computer Science",
                "Java, Spring Boot, SQL, React",
                "Second-year MS student looking for a backend-heavy project.");
        User priya = user("Priya Nair", "priya.nair@demo.aum.edu", Role.STUDENT, "Computer Science",
                "Python, Machine Learning, Pandas",
                "Interested in applied ML and data pipelines.");
        User sam = user("Sam Okafor", "sam.okafor@demo.aum.edu", Role.STUDENT, "Electrical Engineering",
                "C, Arduino, PCB design",
                "Undergrad, likes hardware projects.");

        if (projectRepository.findByOwnerId(maya.getId()).isEmpty()) {
            Project tracker = project(maya, "Course Registration Waitlist Tracker",
                    "A web app that watches course seats and notifies students when a seat opens. "
                            + "Needs a scheduler, a small REST API and email notifications.",
                    "Web", "Java, Spring Boot, PostgreSQL", ProjectStatus.OPEN);
            Project cache = project(maya, "Benchmarking Key-Value Stores",
                    "Compare Redis, RocksDB and an in-house LRU cache under read-heavy workloads "
                            + "and write up p50/p95/p99 latency results.",
                    "Research", "C++, Linux, Benchmarking", ProjectStatus.OPEN);
            Project library = project(maya, "Library Room Booking",
                    "Booking system for library study rooms. First version is live, so this one is closed.",
                    "Web", "React, Node.js", ProjectStatus.CLOSED);

            request(tracker, alex, "I built a Spring Boot API for my databases class and would like to work on the scheduler.",
                    RequestStatus.PENDING);
            request(tracker, priya, "Happy to help with the notification side and testing.", RequestStatus.ACCEPTED);
            request(cache, alex, "I've used perf before and want to learn more about caching.", RequestStatus.REJECTED);
            request(library, sam, "Is there anything left to do on the hardware side?", RequestStatus.REJECTED);
            log.info("Demo projects created for {}", maya.getEmail());
        }

        if (projectRepository.findByOwnerId(james.getId()).isEmpty()) {
            Project energy = project(james, "Campus Energy Sensors",
                    "Build and deploy cheap power sensors in two campus buildings and stream readings "
                            + "to a dashboard.",
                    "IoT", "C, Arduino, MQTT", ProjectStatus.OPEN);
            Project predict = project(james, "Predicting HVAC Load",
                    "Use a year of building data to predict next-day heating and cooling load.",
                    "Data Science", "Python, Machine Learning, Pandas", ProjectStatus.OPEN);
            project(james, "Smart Parking Prototype",
                    "Ultrasonic sensors that report free parking spots to a mobile app.",
                    "Mobile", "Flutter, C, Bluetooth", ProjectStatus.OPEN);

            request(energy, sam, "I have done two Arduino projects and can solder.", RequestStatus.ACCEPTED);
            request(predict, priya, "This lines up with my ML coursework. I'd love to try gradient boosting on it.",
                    RequestStatus.PENDING);
            log.info("Demo projects created for {}", james.getEmail());
        }
    }

    private User user(String name, String email, Role role, String department, String skills, String bio) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User(name, email, passwordEncoder.encode(PASSWORD), role);
            user.setDepartment(department);
            user.setSkills(skills);
            user.setBio(bio);
            return userRepository.save(user);
        });
    }

    private Project project(User owner, String title, String description, String category, String skills,
                            ProjectStatus status) {
        Project project = new Project(title, description, category, skills, owner);
        project.setStatus(status);
        return projectRepository.save(project);
    }

    private void request(Project project, User user, String message, RequestStatus status) {
        JoinRequest request = new JoinRequest(project, user, message);
        request.setStatus(status);
        joinRequestRepository.save(request);
    }
}
