# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "queued_images" ("id" SERIAL NOT NULL PRIMARY KEY,"image_url" VARCHAR(254) NOT NULL,"size" INTEGER NOT NULL,"source_url" VARCHAR(254) NOT NULL,"created_at" BIGINT NOT NULL);

# --- !Downs

drop table "queued_images";

