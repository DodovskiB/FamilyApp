begin;
insert into auth.users(id) values('f6922000-0000-4000-8000-000000000001'),('f6922000-0000-4000-8000-000000000002');
set local role authenticated;
select set_config('request.jwt.claim.sub','f6922000-0000-4000-8000-000000000001',true);
do $$ declare rows jsonb; before_body jsonb; after_body jsonb;
begin
 perform public.shopping_sync_apply('[
 {"operation_id":"f6922000-0000-4000-8000-000000000011","entity":"list","id":"f6922000-0000-4000-8000-000000000021","action":"insert","body":{"name":"Home","sort_order":0}},
 {"operation_id":"f6922000-0000-4000-8000-000000000012","entity":"list","id":"f6922000-0000-4000-8000-000000000022","action":"insert","body":{"name":"Home","sort_order":1}},
 {"operation_id":"f6922000-0000-4000-8000-000000000013","entity":"item","id":"f6922000-0000-4000-8000-000000000023","action":"insert","body":{"title":"Disposable product","category":"Other","qty":4,"checked":true,"active":true,"list_id":"f6922000-0000-4000-8000-000000000021","catalog_id":null}}]');
 select value->'body' into before_body from jsonb_array_elements(public.shopping_sync_pull(0)) where value->>'entity'='item';
 perform public.shopping_sync_merge_lists('f6922000-0000-4000-8000-000000000021','f6922000-0000-4000-8000-000000000022');
 perform public.shopping_sync_merge_lists('f6922000-0000-4000-8000-000000000021','f6922000-0000-4000-8000-000000000022');
 rows:=public.shopping_sync_pull(0);
 if (select count(*) from jsonb_array_elements(rows) where value->>'entity'='list' and not (value->>'deleted')::boolean)<>1 then raise exception 'Merge/retry failed'; end if;
 select value->'body' into after_body from jsonb_array_elements(rows) where value->>'entity'='item';
 if before_body-'list_id' is distinct from after_body-'list_id' then raise exception 'Item fields changed'; end if;
 if after_body->>'list_id'<>'f6922000-0000-4000-8000-000000000022' then raise exception 'Entry not moved'; end if;
 perform public.shopping_sync_apply('[{"operation_id":"f6922000-0000-4000-8000-000000000014","entity":"item","id":"f6922000-0000-4000-8000-000000000024","action":"insert","body":{"title":"Offline product","category":"Other","qty":2,"checked":false,"active":true,"list_id":"f6922000-0000-4000-8000-000000000021","catalog_id":null}}]');
 if (select count(*) from jsonb_array_elements(public.shopping_sync_pull(0)) where value->>'entity'='item' and value->'body'->>'list_id'='f6922000-0000-4000-8000-000000000022')<>2 then raise exception 'Old device parent redirect failed'; end if;
 perform set_config('request.jwt.claim.sub','f6922000-0000-4000-8000-000000000002',true);
 begin
   perform public.shopping_sync_merge_lists('f6922000-0000-4000-8000-000000000021','f6922000-0000-4000-8000-000000000022');
   raise exception 'Other account merge allowed';
 exception when insufficient_privilege then null; end;
end $$;
reset role;
rollback;
select 'PASS: merge preserves all entry fields, retry is idempotent, old-device inserts redirect, other accounts denied; fixtures rolled back' as result;
