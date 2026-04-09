-- V057: Create spare_machine_assignment table
-- Tracks which real cab a spare machine is assigned to and for how long

CREATE TABLE spare_machine_assignment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  spare_machine_id BIGINT NOT NULL,
  real_cab_number INT NOT NULL,
  assigned_at DATETIME NOT NULL,
  returned_at DATETIME,
  notes VARCHAR(500),
  created_by VARCHAR(255),
  created_at DATETIME NOT NULL,
  updated_at DATETIME,
  FOREIGN KEY (spare_machine_id) REFERENCES spare_machine(id),
  INDEX idx_real_cab_time (real_cab_number, assigned_at, returned_at),
  INDEX idx_active_assignments (returned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


