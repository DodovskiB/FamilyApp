-- Run in the SQL editor after the migration. All fixture users/data roll back.
begin;
insert into auth.users(id) values
 ('f6921000-0000-4000-8000-000000000001'),('f6921000-0000-4000-8000-000000000002');
set local role authenticated;
select set_config('request.jwt.claim.sub','f6921000-0000-4000-8000-000000000001',true);
do $$
declare ops jsonb; n integer;
begin
  ops:='[{"operation_id":"f6921000-0000-4000-8000-000000000011","entity":"list","id":"f6921000-0000-4000-8000-000000000021","action":"insert","body":{"name":"Disposable sync test","sort_order":0}},{"operation_id":"f6921000-0000-4000-8000-000000000012","entity":"item","id":"f6921000-0000-4000-8000-000000000022","action":"insert","body":{"title":"Test product","category":"Other","qty":1,"checked":false,"active":true,"list_id":"f6921000-0000-4000-8000-000000000021","catalog_id":null}}]';
  perform public.shopping_sync_apply(ops);
  perform public.shopping_sync_apply(ops);
  if jsonb_array_length(public.shopping_sync_pull(0))<>2 then raise exception 'Insert replay duplicated records'; end if;
  ops:='[{"operation_id":"f6921000-0000-4000-8000-000000000013","entity":"item","id":"f6921000-0000-4000-8000-000000000022","action":"update","body":{"qty_delta":1}}]';
  perform public.shopping_sync_apply(ops); perform public.shopping_sync_apply(ops);
  ops:=jsonb_set(ops,'{0,operation_id}','"f6921000-0000-4000-8000-000000000014"');
  perform public.shopping_sync_apply(ops);
  select (value->'body'->>'qty')::integer into n from jsonb_array_elements(public.shopping_sync_pull(0)) where value->>'entity'='item';
  if n<>3 then raise exception 'Quantity delta/replay failed'; end if;
  perform set_config('request.jwt.claim.sub','f6921000-0000-4000-8000-000000000002',true);
  if public.shopping_sync_pull(0)<>'[]'::jsonb then raise exception 'RPC account isolation failed'; end if;
  if exists(select 1 from public.shopping_sync_records) then raise exception 'RLS account isolation failed'; end if;
  begin
    insert into public.shopping_sync_records(owner_id,entity,id,body)
      values('f6921000-0000-4000-8000-000000000002','list',gen_random_uuid(),'{}');
    raise exception 'Direct writes unexpectedly allowed';
  exception when insufficient_privilege then null; end;
  perform set_config('request.jwt.claim.sub','f6921000-0000-4000-8000-000000000001',true);
  perform public.shopping_sync_apply('[{"operation_id":"f6921000-0000-4000-8000-000000000015","entity":"list","id":"f6921000-0000-4000-8000-000000000021","action":"delete","body":{}}]');
  if exists(select 1 from jsonb_array_elements(public.shopping_sync_pull(0)) where not (value->>'deleted')::boolean) then
    raise exception 'List deletion did not cascade'; end if;
  perform public.shopping_sync_apply('[{"operation_id":"f6921000-0000-4000-8000-000000000016","entity":"item","id":"f6921000-0000-4000-8000-000000000022","action":"update","body":{"checked":true}}]');
  if exists(select 1 from jsonb_array_elements(public.shopping_sync_pull(0)) where not (value->>'deleted')::boolean) then
    raise exception 'Offline edit resurrected deletion'; end if;
end $$;
reset role;
rollback;
select 'PASS: account isolation, RLS, direct-write protection, replay, quantity deltas, deletion cascade and no resurrection; test data rolled back' as result;
