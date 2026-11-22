
USE `computo_examen`;

DROP TABLE IF EXISTS `ia_dato_entrenamiento`;

CREATE TABLE `ia_dato_entrenamiento` (
  `dato_id` INT NOT NULL AUTO_INCREMENT,
  `interes` ENUM('ambos','salud','apariencia') NOT NULL,
  `ejercicio` ENUM('sedentario','moderado','activo') NOT NULL,
  `motivacion` ENUM('moderado','muy motivado') NOT NULL,
  `tecnologia` ENUM('si','no') NOT NULL,
  `modelo` ENUM('i100','i500') NOT NULL,
  PRIMARY KEY (`dato_id`))
ENGINE = InnoDB DEFAULT CHARACTER SET = utf8;

CREATE TABLE IF NOT EXISTS `ia_consulta` (
  `consulta_id` INT NOT NULL AUTO_INCREMENT,
  `id_publico` VARCHAR(45) NULL,
  `interes` VARCHAR(20) NOT NULL,
  `ejercicio` VARCHAR(20) NOT NULL,
  `motivacion` VARCHAR(20) NOT NULL,
  `tecnologia` VARCHAR(5) NOT NULL,
  `prob_i100` DOUBLE NOT NULL,
  `prob_i500` DOUBLE NOT NULL,
  `recomendado` VARCHAR(10) NOT NULL,
  `fecha_hora` DATETIME NOT NULL,
  PRIMARY KEY (`consulta_id`))
ENGINE = InnoDB DEFAULT CHARACTER SET = utf8;

INSERT INTO `ia_dato_entrenamiento` (`interes`,`ejercicio`,`motivacion`,`tecnologia`,`modelo`) VALUES
('ambos','sedentario','moderado','si','i100'),
('ambos','sedentario','moderado','no','i100'),
('salud','sedentario','moderado','si','i500'),
('apariencia','activo','moderado','si','i500'),
('apariencia','moderado','muy motivado','si','i500'),
('apariencia','moderado','muy motivado','no','i100'),
('salud','moderado','muy motivado','no','i500'),
('ambos','activo','moderado','si','i100'),
('ambos','moderado','muy motivado','si','i500'),
('apariencia','activo','muy motivado','si','i500'),
('ambos','activo','muy motivado','no','i500'),
('salud','activo','moderado','no','i500'),
('salud','sedentario','muy motivado','si','i500'),
('apariencia','activo','moderado','no','i100'),
('salud','sedentario','moderado','no','i100');
