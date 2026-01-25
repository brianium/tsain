-- Initial schema for tsain component storage
-- Version: 001
-- Date: 2026-01-24

-- Components table (no props - html.yeah provides those)
CREATE TABLE IF NOT EXISTS components (
  id INTEGER PRIMARY KEY,
  tag TEXT UNIQUE NOT NULL,
  category TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
)
--;;

-- Component examples (1:many)
CREATE TABLE IF NOT EXISTS examples (
  id INTEGER PRIMARY KEY,
  component_id INTEGER NOT NULL REFERENCES components(id) ON DELETE CASCADE,
  label TEXT NOT NULL,
  hiccup TEXT NOT NULL,
  sort_order INTEGER DEFAULT 0
)
--;;

-- Index for faster example lookups
CREATE INDEX IF NOT EXISTS idx_examples_component_id ON examples(component_id)
--;;

-- Full-text search virtual table
CREATE VIRTUAL TABLE IF NOT EXISTS components_fts USING fts5(
  tag,
  category,
  content='components',
  content_rowid='id'
)
--;;

-- Trigger: sync FTS on insert
CREATE TRIGGER IF NOT EXISTS components_ai AFTER INSERT ON components BEGIN
  INSERT INTO components_fts(rowid, tag, category) VALUES (new.id, new.tag, new.category);
END
--;;

-- Trigger: sync FTS on delete
CREATE TRIGGER IF NOT EXISTS components_ad AFTER DELETE ON components BEGIN
  INSERT INTO components_fts(components_fts, rowid, tag, category) VALUES ('delete', old.id, old.tag, old.category);
END
--;;

-- Trigger: sync FTS on update
CREATE TRIGGER IF NOT EXISTS components_au AFTER UPDATE ON components BEGIN
  INSERT INTO components_fts(components_fts, rowid, tag, category) VALUES ('delete', old.id, old.tag, old.category);
  INSERT INTO components_fts(rowid, tag, category) VALUES (new.id, new.tag, new.category);
END
