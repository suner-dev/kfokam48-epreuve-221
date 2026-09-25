INSERT INTO promotion (nom)
SELECT 'Promotion Démo KFOKAM48'
WHERE NOT EXISTS (SELECT 1 FROM promotion WHERE nom = 'Promotion Démo KFOKAM48');

INSERT INTO formateur (nom)
SELECT 'Formateur Démo'
WHERE NOT EXISTS (SELECT 1 FROM formateur WHERE nom = 'Formateur Démo');

INSERT INTO etudiant (nom, prenom, promotion_id)
SELECT 'Dupont', 'Alice', p.id
FROM promotion p
WHERE p.nom = 'Promotion Démo KFOKAM48'
  AND NOT EXISTS (SELECT 1 FROM etudiant e WHERE e.nom = 'Dupont' AND e.prenom = 'Alice' AND e.promotion_id = p.id);

INSERT INTO etudiant (nom, prenom, promotion_id)
SELECT 'Martin', 'Bruno', p.id
FROM promotion p
WHERE p.nom = 'Promotion Démo KFOKAM48'
  AND NOT EXISTS (SELECT 1 FROM etudiant e WHERE e.nom = 'Martin' AND e.prenom = 'Bruno' AND e.promotion_id = p.id);

INSERT INTO etudiant (nom, prenom, promotion_id)
SELECT 'Nkoa', 'Carla', p.id
FROM promotion p
WHERE p.nom = 'Promotion Démo KFOKAM48'
  AND NOT EXISTS (SELECT 1 FROM etudiant e WHERE e.nom = 'Nkoa' AND e.prenom = 'Carla' AND e.promotion_id = p.id);

INSERT INTO session_cours (titre, code, promotion_id, ouverture_at, expiration_at)
SELECT 'Session Démo', 'DEMO2026', p.id, TIMESTAMP '2000-01-01 00:00:00', TIMESTAMP '2099-01-01 00:00:00'
FROM promotion p
WHERE p.nom = 'Promotion Démo KFOKAM48'
  AND NOT EXISTS (SELECT 1 FROM session_cours s WHERE s.code = 'DEMO2026');

INSERT INTO presence (session_id, etudiant_id, source, marquee_at)
SELECT s.id, e.id, 'ETUDIANT', CURRENT_TIMESTAMP
FROM session_cours s
JOIN etudiant e ON e.nom = 'Dupont' AND e.prenom = 'Alice'
WHERE s.code = 'DEMO2026'
  AND NOT EXISTS (SELECT 1 FROM presence p WHERE p.session_id = s.id AND p.etudiant_id = e.id);

INSERT INTO presence (session_id, etudiant_id, source, marquee_at)
SELECT s.id, e.id, 'ETUDIANT', CURRENT_TIMESTAMP
FROM session_cours s
JOIN etudiant e ON e.nom = 'Martin' AND e.prenom = 'Bruno'
WHERE s.code = 'DEMO2026'
  AND NOT EXISTS (SELECT 1 FROM presence p WHERE p.session_id = s.id AND p.etudiant_id = e.id);

INSERT INTO presence (session_id, etudiant_id, source, marquee_at)
SELECT s.id, e.id, 'FORMATEUR', CURRENT_TIMESTAMP
FROM session_cours s
JOIN etudiant e ON e.nom = 'Nkoa' AND e.prenom = 'Carla'
WHERE s.code = 'DEMO2026'
  AND NOT EXISTS (SELECT 1 FROM presence p WHERE p.session_id = s.id AND p.etudiant_id = e.id);

INSERT INTO exercice (session_id, etudiant_id, lien, statut, depose_at)
SELECT s.id, e.id, 'https://example.test/exercices/demo-alice', 'EN_ATTENTE_DE_RELECTURE', CURRENT_TIMESTAMP
FROM session_cours s
JOIN etudiant e ON e.nom = 'Dupont' AND e.prenom = 'Alice'
WHERE s.code = 'DEMO2026'
  AND NOT EXISTS (SELECT 1 FROM exercice x WHERE x.session_id = s.id AND x.etudiant_id = e.id);

INSERT INTO relecture (exercice_id, relecteur_id, note, commentaire, commencee_at, rendue_at)
SELECT x.id, r.id, NULL, NULL, NULL, NULL
FROM exercice x
JOIN session_cours s ON s.id = x.session_id
JOIN etudiant a ON a.id = x.etudiant_id AND a.nom = 'Dupont' AND a.prenom = 'Alice'
JOIN etudiant r ON r.nom = 'Martin' AND r.prenom = 'Bruno'
WHERE s.code = 'DEMO2026'
  AND x.lien = 'https://example.test/exercices/demo-alice'
  AND NOT EXISTS (SELECT 1 FROM relecture l WHERE l.exercice_id = x.id);
