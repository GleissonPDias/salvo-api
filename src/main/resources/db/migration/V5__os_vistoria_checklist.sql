-- Checklist de entrada (12 itens com estado OK/ATENCAO/AVARIA) persistido com a vistoria da OS.
ALTER TABLE os_vistorias ADD COLUMN IF NOT EXISTS checklist jsonb NOT NULL DEFAULT '{}'::jsonb;
