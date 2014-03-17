# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "queued_images" ("created_at" BIGINT NOT NULL,"image_url" VARCHAR NOT NULL,"source_url" VARCHAR NOT NULL);

# --- !Downs

drop table "queued_images";

