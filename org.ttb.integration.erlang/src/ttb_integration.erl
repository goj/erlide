%% Author: Piotr Dorobisz
%% Created: Jul 29, 2010
-module(ttb_integration).

%%
%% Exported Functions
%%
-export([start/2, stop/0,  load/1, load_data/1, str2ms/1]).


start(Nodes, FileName)->
	ttbe:stop(),
	ttbe:tracer(Nodes, [{file,{local, FileName}}]).

stop() ->
	{stopped, Dir} = ttbe:stop([return]),
	spawn(?MODULE, load_data, [Dir]).

load(Path) ->
	spawn(?MODULE, load_data, [Path]).

load_data(Path) ->
	case ttbe:format(Path, [{handler, {create_handler(), initial_state}}]) of
		ok -> erlide_jrpc:event(trace_event, stop_tracing);
		{error, Reason} -> erlide_jrpc:event(trace_event, error_loading)
%% 		A -> erlide_jrpc:event(trace_event, A)
	end.

create_handler() ->
	fun(Fd, Trace, _TraceInfo, State) ->
			case Trace of
				{trace_ts, Pid, call, {Mod, Fun, Args}, Time} ->
					erlide_jrpc:event(trace_event, {trace_ts, Pid, call, {Mod, Fun,[avoid_interpreting_as_string] ++ Args}, calendar:now_to_local_time(Time)});
				{trace_ts, Pid, spawn, Pid2, {M, F, Args}, Time} ->
					erlide_jrpc:event(trace_event, {trace_ts, Pid, spawn, Pid2, {M, F, [avoid_interpreting_as_string] ++ Args}, calendar:now_to_local_time(Time)});
				{trace_ts, _, _, _, Time} ->
					T = calendar:now_to_local_time(Time),
					erlide_jrpc:event(trace_event, setelement(tuple_size(Trace), Trace, T));
				{trace_ts, _, _, _, _, Time} ->
					T = calendar:now_to_local_time(Time),
					erlide_jrpc:event(trace_event, setelement(tuple_size(Trace), Trace, T));
				_ ->
					erlide_jrpc:event(trace_event, Trace)
			end,
			State
	end.

str2fun(S) ->
	case erl_scan:string(S) of
		{error, ErrorInfo, _} ->
			{error, ErrorInfo};
		{ok, Tokens, _} ->
			case erl_parse:parse_exprs(Tokens) of
				{error, ErrorInfo} ->
					{error, ErrorInfo};
				{ok, Expr_list} ->
					{value, Value, _} = erl_eval:exprs(Expr_list, erl_eval:new_bindings()),
					{ok, Value}
			end
	end.

str2ms(S) ->
	try (str2fun(S)) of
		{error, ErrorInfo} ->
			{error, standard_info, ErrorInfo};
		{ok, Fun} ->
			try dbg:fun2ms(Fun) of
				{error, ErrorInfo} -> {error, standard_info, ErrorInfo};
				Result -> {ok, Result}
			catch error:function_clause -> {error, not_fun} end
	catch
		error:{unbound_var, X} -> {error, unbound_var, X}
	end.