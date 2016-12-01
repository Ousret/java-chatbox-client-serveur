CREATE SEQUENCE sessioncliente_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE sessioncliente
(
    id SERIAL PRIMARY KEY,
    utilisateur_id INTEGER REFERENCES utilisateur(id),
    uuid VARCHAR(255) UNIQUE,
    debutSession DATE
);