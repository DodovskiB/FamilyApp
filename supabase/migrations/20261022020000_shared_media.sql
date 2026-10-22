-- Private, list-scoped attachments. Existing sync RPCs/old clients remain compatible.
begin;
create table public.shopping_item_media (
  owner_id uuid not null references auth.users(id) on delete cascade,
  item_id uuid not null,
  link_url text,
  photo_path text,
  primary key(owner_id,item_id)
);
create table public.shopping_media_operations (
  owner_id uuid not null references auth.users(id) on delete cascade,
  operation_id uuid not null,
  request_hash text not null,
  primary key(owner_id,operation_id)
);
alter table public.shopping_item_media enable row level security;
alter table public.shopping_media_operations enable row level security;
revoke all on public.shopping_item_media,public.shopping_media_operations from public,anon,authenticated;

-- Resolve the explicit legacy list merge redirects without exposing private rows to clients.
create function public.shopping_media_resolve(owner uuid, original_list uuid) returns uuid
language plpgsql stable security definer set search_path='' as $$
declare current_list uuid:=original_list; row public.shopping_sync_records%rowtype; hops integer:=0;
begin
  loop
    select * into row from public.shopping_sync_records r where r.owner_id=owner and r.entity='list' and r.id=current_list;
    if not found then return null; end if;
    if not row.deleted then return current_list; end if;
    if row.body->>'merged_into' is null then return null; end if;
    current_list:=(row.body->>'merged_into')::uuid; hops:=hops+1;
    if hops>32 then return null; end if;
  end loop;
end $$;
revoke all on function public.shopping_media_resolve(uuid,uuid) from public,anon,authenticated;

create function public.shopping_media_access(object_name text, for_delete boolean default false) returns boolean
language plpgsql stable security definer set search_path='' as $$
declare subject uuid:=auth.uid(); owner uuid; original_list uuid; target uuid;
begin
  if subject is null or object_name !~ '^[a-f0-9-]{36}/[a-f0-9-]{36}/[a-f0-9]{64}\.jpg$' then return false; end if;
  begin owner:=split_part(object_name,'/',1)::uuid; original_list:=split_part(object_name,'/',2)::uuid;
  exception when invalid_text_representation then return false; end;
  target:=public.shopping_media_resolve(owner,original_list);
  if target is null then
    return for_delete and subject=owner and exists(select 1 from public.shopping_sync_records r
      where r.owner_id=owner and r.entity='list' and r.id=original_list and r.deleted and r.body->>'merged_into' is null);
  end if;
  if subject<>owner and not exists(select 1 from public.shopping_list_members m
    where m.owner_id=owner and m.list_id=target and m.member_id=subject) then return false; end if;
  if not for_delete then return true; end if;
  -- Grace protects an upload not yet attached by its next RPC. Never delete a live reference.
  return exists(select 1 from storage.objects o where o.bucket_id='shopping-photos' and o.name=object_name
      and o.created_at<now()-interval '24 hours')
    and not exists(select 1 from public.shopping_item_media p join public.shopping_sync_records i
      on i.owner_id=p.owner_id and i.entity='item' and i.id=p.item_id and not i.deleted
      where p.photo_path=object_name);
end $$;
revoke all on function public.shopping_media_access(text,boolean) from public,anon;
grant execute on function public.shopping_media_access(text,boolean) to authenticated;

create function public.shopping_media_snapshot() returns jsonb
language plpgsql stable security definer set search_path='' as $$
declare subject uuid:=auth.uid(); result jsonb;
begin
  if subject is null then raise exception 'Authentication required' using errcode='42501'; end if;
  select coalesce(jsonb_agg(jsonb_build_object('id',i.id,'owner_id',i.owner_id,'list_id',l.id,
      'has_media',p.item_id is not null,'link_url',p.link_url,'photo_path',p.photo_path)),'[]'::jsonb) into result
    from public.shopping_sync_records i
    join public.shopping_sync_records l on l.owner_id=i.owner_id and l.entity='list' and l.id=(i.body->>'list_id')::uuid and not l.deleted
    left join public.shopping_item_media p on p.owner_id=i.owner_id and p.item_id=i.id
    where i.entity='item' and not i.deleted and (i.owner_id=subject or exists(
      select 1 from public.shopping_list_members m where m.owner_id=i.owner_id and m.list_id=l.id and m.member_id=subject));
  return result;
end $$;

create function public.shopping_media_apply(owner_id uuid, list_id uuid, item_id uuid, operation_id uuid,
    field_mask integer, link_url text default null, photo_path text default null) returns void
language plpgsql security definer set search_path='' as $$
declare subject uuid:=auth.uid(); previous_hash text; request_hash text;
begin
  if subject is null then raise exception 'Authentication required' using errcode='42501'; end if;
  if owner_id is null or list_id is null or item_id is null or operation_id is null or field_mask not in (1,2,3)
    then raise exception 'Invalid media operation'; end if;
  perform pg_advisory_xact_lock(hashtextextended(owner_id::text,0));
  if not exists(select 1 from public.shopping_sync_records l where l.owner_id=shopping_media_apply.owner_id
    and l.entity='list' and l.id=list_id and not l.deleted) or
    (subject<>owner_id and not exists(select 1 from public.shopping_list_members m where m.owner_id=shopping_media_apply.owner_id
      and m.list_id=shopping_media_apply.list_id and m.member_id=subject))
    then raise exception 'List access removed' using errcode='42501'; end if;
  request_hash:=md5(jsonb_build_array(list_id,item_id,field_mask,link_url,photo_path)::text);
  select o.request_hash into previous_hash from public.shopping_media_operations o
    where o.owner_id=shopping_media_apply.owner_id and o.operation_id=shopping_media_apply.operation_id;
  if found then
    if previous_hash<>request_hash then raise exception 'Operation ID reused'; end if;
    return;
  end if;
  if not exists(select 1 from public.shopping_sync_records i where i.owner_id=shopping_media_apply.owner_id
    and i.entity='item' and i.id=item_id and not i.deleted and i.body->>'list_id'=list_id::text) then
    return; -- A deleted/moved entry must never be resurrected by a delayed photo operation.
  end if;
  if field_mask & 2 <> 0 and link_url is not null and
    (length(link_url)>8192 or link_url !~ '^https?://[^[:space:]]+$') then raise exception 'Invalid link'; end if;
  if field_mask & 1 <> 0 and photo_path is not null then
    if not public.shopping_media_access(photo_path,false) or split_part(photo_path,'/',1)<>owner_id::text
      or public.shopping_media_resolve(owner_id,split_part(photo_path,'/',2)::uuid) is distinct from list_id
      or not exists(select 1 from storage.objects o where o.bucket_id='shopping-photos' and o.name=photo_path)
      then raise exception 'Invalid photo reference' using errcode='42501'; end if;
  end if;
  insert into public.shopping_item_media as p(owner_id,item_id,link_url,photo_path)
    values(owner_id,item_id,case when field_mask & 2 <> 0 then link_url end,case when field_mask & 1 <> 0 then photo_path end)
    on conflict on constraint shopping_item_media_pkey do update set
      link_url=case when field_mask & 2 <> 0 then excluded.link_url else p.link_url end,
      photo_path=case when field_mask & 1 <> 0 then excluded.photo_path else p.photo_path end;
  insert into public.shopping_media_operations values(owner_id,operation_id,request_hash);
end $$;

create function public.shopping_media_orphans() returns jsonb
language plpgsql stable security definer set search_path='' as $$
begin
  if auth.uid() is null then raise exception 'Authentication required' using errcode='42501'; end if;
  return (select coalesce(jsonb_agg(name),'[]'::jsonb) from
    (select o.name from storage.objects o where o.bucket_id='shopping-photos'
      and public.shopping_media_access(o.name,true) order by o.created_at limit 100) obsolete);
end $$;
revoke all on function public.shopping_media_snapshot(),public.shopping_media_orphans(),
  public.shopping_media_apply(uuid,uuid,uuid,uuid,integer,text,text) from public,anon;
grant execute on function public.shopping_media_snapshot(),public.shopping_media_orphans(),
  public.shopping_media_apply(uuid,uuid,uuid,uuid,integer,text,text) to authenticated;

-- Supported bucket configuration; file bytes are only created/deleted through Storage API.
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
  values('shopping-photos','shopping-photos',false,256000,array['image/jpeg']);
create policy shopping_photos_read on storage.objects for select to authenticated
  using(bucket_id='shopping-photos' and public.shopping_media_access(name,false));
create policy shopping_photos_insert on storage.objects for insert to authenticated
  with check(bucket_id='shopping-photos' and public.shopping_media_access(name,false));
create policy shopping_photos_delete on storage.objects for delete to authenticated
  using(bucket_id='shopping-photos' and public.shopping_media_access(name,true));
-- No UPDATE policy: content-addressed image objects are immutable.
commit;
