CREATE SEQUENCE message_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE message
(
    id SERIAL PRIMARY KEY,
    dateCreation DATE,
    utilisateur_id INTEGER REFERENCES utilisateur(id),
    suspendre BOOLEAN,
    message VARCHAR(255),
    salon_id INTEGER REFERENCES salon(id)
);