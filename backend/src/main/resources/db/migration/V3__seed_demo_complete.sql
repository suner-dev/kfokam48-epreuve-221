INSERT INTO etudiant (nom, prenom, promotion_id)
SELECT 'Demo', CONCAT('Etudiant', CAST(n AS VARCHAR)), p.id
FROM (
    VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10),
           (11), (12), (13), (14), (15), (16), (17), (18), (19), (20),
           (21), (22), (23), (24), (25), (26), (27), (28), (29), (30),
           (31), (32), (33), (34), (35), (36), (37), (38), (39), (40),
           (41), (42), (43), (44), (45), (46), (47), (48), (49), (50),
           (51), (52), (53), (54), (55), (56), (57)
) AS numbers(n)
CROSS JOIN promotion p
WHERE p.nom = 'Promotion Démo KFOKAM48'
  AND NOT EXISTS (
      SELECT 1 FROM etudiant e
      WHERE e.nom = 'Demo'
        AND e.prenom = CONCAT('Etudiant', CAST(n AS VARCHAR))
        AND e.promotion_id = p.id
  );

INSERT INTO session_cours (titre, code, promotion_id, ouverture_at, expiration_at)
SELECT 'Session Démo 2', 'DEMO-B', p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1' DAY
FROM promotion p
WHERE p.nom = 'Promotion Démo KFOKAM48'
  AND NOT EXISTS (SELECT 1 FROM session_cours s WHERE s.code = 'DEMO-B');

INSERT INTO presence (session_id, etudiant_id, source, marquee_at)
SELECT s.id, e.id,
       CASE WHEN MOD(n, 2) = 0 THEN 'FORMATEUR' ELSE 'ETUDIANT' END,
       CURRENT_TIMESTAMP
FROM (
    VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10),
           (11), (12), (13), (14), (15), (16), (17), (18), (19), (20),
           (21), (22), (23), (24), (25), (26), (27), (28), (29), (30),
           (31), (32), (33), (34), (35), (36), (37), (38), (39), (40),
           (41), (42), (43), (44), (45), (46), (47), (48), (49), (50),
           (51), (52), (53), (54), (55), (56), (57)
) AS numbers(n)
CROSS JOIN session_cours s
JOIN etudiant e ON e.nom = 'Demo' AND e.prenom = CONCAT('Etudiant', CAST(n AS VARCHAR))
WHERE s.code = 'DEMO-B'
  AND NOT EXISTS (
      SELECT 1 FROM presence p WHERE p.session_id = s.id AND p.etudiant_id = e.id
  );

INSERT INTO exercice (session_id, etudiant_id, lien, statut, depose_at)
SELECT s.id, e.id, 'https://example.test/exercices/demo-relu', 'RELU', CURRENT_TIMESTAMP
FROM session_cours s
JOIN etudiant e ON e.nom = 'Dupont' AND e.prenom = 'Alice'
WHERE s.code = 'DEMO-B'
  AND NOT EXISTS (
      SELECT 1 FROM exercice x WHERE x.session_id = s.id AND x.etudiant_id = e.id
  );

INSERT INTO relecture (exercice_id, relecteur_id, note, commentaire, commencee_at, rendue_at)
SELECT x.id, r.id, 15, 'Relecture relue de démonstration', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM exercice x
JOIN etudiant a ON a.id = x.etudiant_id AND a.nom = 'Dupont' AND a.prenom = 'Alice'
JOIN etudiant r ON r.nom = 'Martin' AND r.prenom = 'Bruno'
WHERE x.lien = 'https://example.test/exercices/demo-relu'
  AND NOT EXISTS (SELECT 1 FROM relecture l WHERE l.exercice_id = x.id);

INSERT INTO exercice (session_id, etudiant_id, lien, statut, depose_at)
SELECT s.id, e.id, 'https://example.test/exercices/demo-attente-avec-relecteur', 'EN_ATTENTE_DE_RELECTURE', CURRENT_TIMESTAMP
FROM session_cours s
JOIN etudiant e ON e.nom = 'Martin' AND e.prenom = 'Bruno'
WHERE s.code = 'DEMO-B'
  AND NOT EXISTS (
      SELECT 1 FROM exercice x WHERE x.session_id = s.id AND x.etudiant_id = e.id
  );

INSERT INTO relecture (exercice_id, relecteur_id, note, commentaire, commencee_at, rendue_at)
SELECT x.id, r.id, NULL, NULL, NULL, NULL
FROM exercice x
JOIN etudiant a ON a.id = x.etudiant_id AND a.nom = 'Martin' AND a.prenom = 'Bruno'
JOIN etudiant r ON r.nom = 'Nkoa' AND r.prenom = 'Carla'
WHERE x.lien = 'https://example.test/exercices/demo-attente-avec-relecteur'
  AND NOT EXISTS (SELECT 1 FROM relecture l WHERE l.exercice_id = x.id);

INSERT INTO exercice (session_id, etudiant_id, lien, statut, depose_at)
SELECT s.id, e.id, 'https://example.test/exercices/demo-attente-sans-relecteur', 'EN_ATTENTE_SANS_RELECTEUR', CURRENT_TIMESTAMP
FROM session_cours s
JOIN etudiant e ON e.nom = 'Nkoa' AND e.prenom = 'Carla'
WHERE s.code = 'DEMO-B'
  AND NOT EXISTS (
      SELECT 1 FROM exercice x WHERE x.session_id = s.id AND x.etudiant_id = e.id
  );

INSERT INTO relecture (exercice_id, relecteur_id, note, commentaire, commencee_at, rendue_at)
SELECT x.id, NULL, NULL, NULL, NULL, NULL
FROM exercice x
WHERE x.lien = 'https://example.test/exercices/demo-attente-sans-relecteur'
  AND NOT EXISTS (SELECT 1 FROM relecture l WHERE l.exercice_id = x.id);
