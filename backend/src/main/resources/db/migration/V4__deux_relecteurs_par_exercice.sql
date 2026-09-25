-- Enveloppe étape 3 — changement de besoin : « chaque exercice est relu par deux pairs
-- différents, la note retenue est la moyenne des deux ».
--
-- Q6 (« un seul relecteur par exercice ») est ANNULÉE. La contrainte qui la portait,
-- UNIQUE (exercice_id), devient UNIQUE (exercice_id, relecteur_id) : deux affectations
-- au plus par exercice, et jamais deux fois le même pair.
--
-- Cette migration est AJOUTÉE, V1 n'est pas modifiée. Elle est idempotente sur un schéma
-- déjà appliqué et elle survit à une base déjà remplie : aucune donnée existante n'est
-- supprimée, les relectures déjà créées restent des affectations valides.
--
-- Rappel PostgreSQL : en SQL, une contrainte UNIQUE ne considère pas deux NULL comme
-- égaux. Les affectations « sans relecteur » (H3, relecteur_id IS NULL) peuvent donc
-- coexister, ce qui est le comportement voulu.

ALTER TABLE relecture DROP CONSTRAINT uk_relecture_exercice;

ALTER TABLE relecture ADD CONSTRAINT uk_relecture_exercice_relecteur UNIQUE (exercice_id, relecteur_id);

-- Index de couverture de la nouvelle lecture : la moyenne et le décompte des notes
-- rendues par exercice se font par agrégation sur cet axe.
CREATE INDEX idx_relecture_exercice_rendue ON relecture (exercice_id, rendue_at);
