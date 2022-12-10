alter table ConfigurationApplication
    drop constraint configurationapplication_name_key;

create unique index ConfigurationApplication_name on ConfigurationApplication (name) where deleted = false;