# DOSW Laboratory 4 — README Project

**Course:** DOSW — Software Development and Operations

**Institution:** Escuela Colombiana de Ingeniería Julio Garavito

**Activity:** Express Hackathon 2026-2

**Work Mode:** Teams of three students

---

### Team Information & Contributions

| Name                            | GitHub Username |
|---------------------------------|-----------------|
| Daniel Santiago Morales Perdomo | `Fukuyaku88`    |
| Edgar Daniel Ruiz Patiño        | `DaniRuiz11`    |
| Miguel Ángel Acero Laverde      | `Miguelim101`   |
| Rodrigo Gualtero                | `Rodrigoreg92`  |

## 1. Laboratory Objective

This laboratory aims to apply planning tools using the agile frameworks Scrum and Kanban, based on a practical case study, allowing students to master the third phase of the software development life cycle.

---

## 2. Prerequisites

Before getting started, please consider the following recommendations:

* Each member must create a Jira account.
* One of the team members must create a workspace in Jira named: DOSW_Lab4_FirstInitialOfEachMember.
* Create a team in Jira and add the professor (see videos [6](https://pruebacorreoescuelaingeduco.sharepoint.com/:v:/s/2026-1-DOSW-3/IQBIkc3ZzhHRSb6gUyga53XxAfxxph6qbvdAtmp4oSSa3bw?e=0HcjPp) and [7](https://pruebacorreoescuelaingeduco.sharepoint.com/:v:/s/2026-1-DOSW-3/IQBanuI5VDcPQY0AHYFh1ewdAZT9rj9kbKpM3n-G9f1xH5w?e=ah9zW1)): laura.herrera@escuelaing.edu.co.
* Create a repository on GitHub with the name: DOSW_Lab4_FirstInitialOfEachMember. Important: Create the repository with the options to include the README and .gitignore files.
* Create the `develop` branch from `main`.
* Don't forget to add the professor to the repository and send the repository URL via Teams.
* Be sure to distribute the commits appropriately to make the work done by each team member visible.
* Consider the following tools for conducting Planning Poker: Miro or [Planning Poker Online](https://planningpokeronline.com/).

---

## 3. Case Study

**TechCup** is a digital platform for managing the semester-long soccer tournament organized for the Systems Engineering, Artificial Intelligence Engineering, Cybersecurity Engineering, and Statistical Engineering programs at Escuela Colombiana de Ingeniería Julio Garavito. The system will be used by students, team captains, and tournament organizers.

The TechCup logo is available at the following [link](https://github.com/DOSW-2026-2/Requirements/blob/9e0d4cd367cad363ac73cac08c0a52b802e1b3ef/docs/TechCup_Logo.png).

In this first version of the system, TechCup must provide the basic functionality required to create tournaments and register teams for each tournament.

The goal is to have this first version by January 2027, which means in 5 sprints (starting from the date of the lab). Note: Assume each sprint is 4 weeks long.

### Problem Description

Currently, the School does not have a centralized system that allows users to:

* Create a tournament by specifying its basic rules and information (dates, fees, etc.).
* Easily register teams for tournaments.
* Process the registration payment for each team.
* Validate payments made by teams.
* View the teams registered for each tournament.
* Generate reports on registrations for each tournament.
* Send registration payment reports in JSON format to the Dean's Office.

The goal of the system is to ensure that tournaments and registrations comply with specific rules while allowing users to interact with the platform in a simple and secure manner.

### General Business Rules

During interviews with the Dean's Office of the Systems Engineering program, the following basic business rules were identified:

* Tournaments must not last longer than one day.
* Each tournament must be identified by a unique ID consisting of exactly **five digits**, based on the year and academic semester. For example, **20262** represents the second semester of 2026.
* The possible statuses for a tournament are: **Pending, Active, In Progress, Closed, and Cancelled**.
* Only one tournament can be active at a time.
* Each team may register only for the tournament that is currently active.
* Teams must be able to pay the registration fee through **PSE**.
* Tournaments cannot be deleted.

### General Functionalities

The main functionalities of **TechCup** are:

* User authentication using username and password for TechCup organizers and students.
* **Tournament Management Module:** Authorized users (organizers) should be able to create tournaments, change their status, and update tournament information.
* **Team Management Module:** users should be able to create teams, update team information, make registration payments, and register teams for a tournament. These actions may be performed according to the user's role: **captains** can create teams, make payments, and update team information, while **organizers** can perform all actions, including reviewing payments and approving registrations.
* **View the payment made by a team:** Tournament organizers should be able to consult and verify the payment associated with a team's registration.
* **Generate a report of registered teams:** Tournament organizers should be able to generate a report containing the teams registered for a tournament.
* **Generate a report of registration revenue:** Tournament organizers should be able to generate a report of the revenue obtained from registration fees.
* **Delete a tournament and its registered teams.**

---

## 4. PART 1 – Project Structure (5%)

With the goal of gaining a better understanding of the different uses of Maven, you will create a project using _Maven archetypes_.

1. Create the `feature/proj-structure` branch based on the `develop` branch.
2. Create the basic project structure using the `maven-archetype-quickstart` archetype ([reference](https://maven.apache.org/archetypes/maven-archetype-quickstart/index.html)).

  1. When running the command to create the project using the archetype, make sure to define:
    * `groupId`: `edu.eci.dosw.lab`
    * `artifactId`: `DOSW-Laboratorio4`
  2. In the README.md file, write the full names of the team members and the objective of the lab.
  3. Manually add the *docs* package (including its subfolders) to obtain the following structure:

  ```text
DOSW-Lab4/
├── pom.xml
├── .gitignore
├── README.md
└── src/
|    ├── main/
|    │   └── java/
|    │       └── edu/
|    │           └── eci/
|    │               └── dosw/
|    │                   └── lab/
|    │                       ├── Application.java
|    └── test/
|        └── java/
|            └── edu/
|                └── eci/
|                    └── dosw/
|                        └── lab/
|                           ├── ApplicationTest.java
└── docs/
    ├── uml/
    ├── images/
    ├── requirements/
    ├── planning/
```
5. Create a pull request to merge the changes from the `feature/proj-structure` branch into `develop`. The pull request must be approved by at least one team member.

---

## 5. PART 2 – Work Breakdown for TechCup (30%)

Based on the case study and the requirements defined in parts 3 and 4 of Lab 4, complete the following:

1. Create the `feature/proj-work-breakdown` branch based on the `develop` branch.
2. Define one of the Scrum team roles for each member: Product Owner, Scrum Master, or Developer. Write the assigned roles in the README.md file.
3. Create the `scrum_work_breakdown.md` [file](https://github.com/lauherrerac/dosw-lab4-example/blob/81b94c76ca098a6f0f27df71e9b949fcdd251719/docs/planning/scrum_work_breakdown.md) in the `planning` folder (path: `DOSW-Lab4/docs/planning`). Note: Use the linked document as a template.
4. Identify the epic associated with the requirement you selected in part 4 of Lab 3 and document it in the `scrum_work_breakdown.md` file.
5. Based on the selected epic, describe 4 user stories and document them in the `scrum_work_breakdown.md` file. Important: In this step, do not define the priority or the estimation for the user stories.
6. For each user story, identify at least 3 tasks and document them in the `scrum_work_breakdown.md` file.
7. The Product Owner must define the priority for each of the user stories. Assign a value of High, Medium, or Low to each one. Update the `scrum_work_breakdown.md` file with this definition and add a short justification.
8. Create a pull request to submit the changes from the `feature/proj-work-breakdown` branch to `develop`. This pull request must be approved by at least one team member. Important: The pull request cannot be approved by the same person who created it.

---

## 6. PART 3 – Definition of the Product Backlog in Jira (20%)

1. Create the `feature/jira-backlog` branch based on the `develop` branch.
2. Create the `jira.md` [file](https://github.com/lauherrerac/dosw-lab4-example/blob/39cb04e220dbc275a0b05124438574c968047f8c/docs/planning/jira.md) in the `planning` folder (path: `DOSW-Lab4/docs/planning`). Note: Use the linked document as a template.
3. In the Jira workspace, create the epic selected in Part 2 of the lab. Fill out the fields: title, description, and due date (keep in mind the expected completion date indicated in the case study).
4. Take a screenshot of the created epic and add it to the `jira.md` file.
5. Create the 4 user stories identified in Part 2 of the lab. For each story, fill out the fields: title and description.
6. Take a screenshot of each created user story and add it to the `jira.md` file.
7. Create the 12 tasks identified in Part 2 of the lab. For each task, fill out: title, description, and related activities.
8. Take a screenshot of each created task and add it to the `jira.md` file.
9. Update the `scrum_work_breakdown.md` file with the epic, user story, and task identifiers as assigned by Jira.
10. Take a screenshot of the timeline in Jira and add it to the `jira.md` file.
11. Create a pull request to submit the changes from the `feature/jira-backlog` branch to `develop`. This pull request must be approved by at least one team member. Important: The pull request cannot be approved by the same person who created it.

---

## 7. PART 4 – Story Point Estimation (Planning Poker) (25%)

Based on the case study, the activity breakdown, and the priority of each user story, complete the following:

1. Create the `feature/estimation` branch based on the `develop` branch.
2. Using the Planning Poker technique, estimate the 4 defined user stories. To do this:

  1. The Scrum Master will lead the exercise.
  2. All 3 team members must participate in the voting for each user story.
  3. Record a video of the estimation for at least one of the user stories, showing the dynamic used to reach the points for that story. Important:     It is not necessary to record all 4 estimations.


3. Update the Estimated Story Points field in Jira for each user story.
4. In the `scrum_work_breakdown.md` file, add the video to the user story you recorded.
5. In the `README.md` file, answer the following questions:

  1. What was the biggest difficulty when estimating?
  2. Was it easy to reach a consensus?
  3. How did you resolve scenarios where the estimates for the same user story were not close?

6. Create a pull request to submit the changes from the `feature/estimation` branch to `develop`.
7. One of the team members will review the pull request and leave a mandatory correction comment. The comment must be: "The user stories need to be updated with the estimations defined in Jira." *(Note: If your repo communication should remain in Spanish, use the original: "Falta actualizar las historias de usuario con las estimaciones definidas en Jira")*.
8. The creator of the pull request must update the `scrum_work_breakdown.md` file with the estimations defined in Jira for each user story.
9. The team member who left the comment will review it again and give their decision to approve it or not. Important: The pull request cannot be approved by the same person who created it.

---

## 8. PART 5 – Planning (15%)

Based on the priority given to each user story and the estimations made, complete the following:

1. Create the `feature/planning` branch based on the `develop` branch.
2. In Jira, add the user story or stories that can be developed in the first sprint.
3. Assign a person to each task you planned for the sprint.
4. Take a screenshot of the sprint and add it to the `jira.md` file in the Sprint Backlog section. Indicate why you decided on this planning.
5. Create a pull request to submit the changes from the `feature/planning` branch to `develop`. This pull request must be approved by at least one team member. Important: The pull request cannot be approved by the same person who created it.

---

## 9. PART 6 – Delivery (5%)

Create a pull request to submit the changes from the `develop` branch to `main`. This pull request must be approved by at least one team member. Important: The pull request cannot be approved by the same person who created it.