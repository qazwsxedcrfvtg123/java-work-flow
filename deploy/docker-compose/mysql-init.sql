-- This file is used to initialize the MySQL databases for our services

-- Create workflow database
CREATE DATABASE IF NOT EXISTS workflow_db;
USE workflow_db;

-- Create notification database
CREATE DATABASE IF NOT EXISTS notification_db;
USE notification_db;

-- The actual tables will be created by Flyway migrations