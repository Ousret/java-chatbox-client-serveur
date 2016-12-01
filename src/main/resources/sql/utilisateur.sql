CREATE SEQUENCE utilisateur_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE utilisateur
(
    id SERIAL PRIMARY KEY,
    pseudo VARCHAR(255) UNIQUE,
    dateCreation DATE,
    secret VARCHAR(255) UNIQUE
);