CREATE SEQUENCE salon_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE salon
(
    id SERIAL PRIMARY KEY,
    utilisateur_id INTEGER REFERENCES utilisateur(id),
    designation VARCHAR(255),
    prive BOOLEAN,
    dateCreation DATE
);