/** -*- mode:java; tab-width: 4 -*-
 * This file is part of Erjang - A JVM-based Erlang VM
 *
 * Copyright (c) 2010 by Trifork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package erjang.beam.interpreter;

import java.util.List;

import erjang.EModule;
import erjang.EModuleManager;
import erjang.EFun;
import erjang.EFunHandler;
import erjang.FunID;

import erjang.ERT;
import erjang.EProc;

import erjang.EObject;
import erjang.EAtom;
import erjang.ENumber;
import erjang.EInteger;
import erjang.ESmall;
import erjang.EDouble;
import erjang.ESeq;
import erjang.ECons;
import erjang.ETuple;
import erjang.EBinary;
import erjang.EBitString;
import erjang.EString;

import erjang.EBinMatchState;
import erjang.ErlangException;
import erjang.EBitStringBuilder;
import erjang.m.erlang.ErlBif;

import erjang.beam.BeamFileData;
import erjang.beam.FunctionVisitor;
import erjang.beam.BlockVisitor;
import erjang.beam.BIFUtil;
import erjang.beam.BuiltInFunction;

import erjang.beam.repr.Insn;
import erjang.beam.repr.Operands;
import erjang.beam.repr.ExtFun;

import kilim.Pausable;

public class Interpreter extends AbstractInterpreter {
	public static final short OPC_K_return = 0;
	public static final short OPC_send = 1;
	public static final short OPC_remove_message = 2;
	public static final short OPC_if_end = 3;
	public static final short OPC_func_info_1c_2c_3I = 4;
	public static final short OPC_badmatch = 5;
	public static final short OPC_case_end = 6;
	public static final short OPC_try_case_end = 7;
	public static final short OPC_allocate_1I = 8;
	public static final short OPC_deallocate_1I = 9;
	public static final short OPC_allocate_zero_1I = 10;
	public static final short OPC_allocate_heap_1I = 11;
	public static final short OPC_allocate_heap_zero_1I = 12;
	public static final short OPC_init_1x = 13;
	public static final short OPC_init_1y = 14;
	public static final short OPC_trim_1I = 15;
	public static final short OPC_move_2x = 16;
	public static final short OPC_move_2y = 17;
	public static final short OPC_put_list_3x = 18;
	public static final short OPC_put_list_3y = 19;
	public static final short OPC_get_list_2x_3x = 20;
	public static final short OPC_get_list_2x_3y = 21;
	public static final short OPC_get_list_2y_3x = 22;
	public static final short OPC_get_list_2y_3y = 23;
	public static final short OPC_get_tuple_element_2I_3x = 24;
	public static final short OPC_get_tuple_element_2I_3y = 25;
	public static final short OPC_put_tuple_1I_2x = 26;
	public static final short OPC_put_tuple_1I_2y = 27;
	public static final short OPC_put = 28;
	public static final short OPC_set_tuple_element_2x_3I = 29;
	public static final short OPC_set_tuple_element_2y_3I = 30;
	public static final short OPC_jump_1L = 31;
	public static final short OPC_is_integer_2x_1L = 32;
	public static final short OPC_is_integer_2y_1L = 33;
	public static final short OPC_is_float_2x_1L = 34;
	public static final short OPC_is_float_2y_1L = 35;
	public static final short OPC_is_number_2x_1L = 36;
	public static final short OPC_is_number_2y_1L = 37;
	public static final short OPC_is_atom_2x_1L = 38;
	public static final short OPC_is_atom_2y_1L = 39;
	public static final short OPC_is_pid_2x_1L = 40;
	public static final short OPC_is_pid_2y_1L = 41;
	public static final short OPC_is_reference_2x_1L = 42;
	public static final short OPC_is_reference_2y_1L = 43;
	public static final short OPC_is_port_2x_1L = 44;
	public static final short OPC_is_port_2y_1L = 45;
	public static final short OPC_is_nil_2x_1L = 46;
	public static final short OPC_is_nil_2y_1L = 47;
	public static final short OPC_is_binary_2x_1L = 48;
	public static final short OPC_is_binary_2y_1L = 49;
	public static final short OPC_is_list_2x_1L = 50;
	public static final short OPC_is_list_2y_1L = 51;
	public static final short OPC_is_nonempty_list_2x_1L = 52;
	public static final short OPC_is_nonempty_list_2y_1L = 53;
	public static final short OPC_is_tuple_2x_1L = 54;
	public static final short OPC_is_tuple_2y_1L = 55;
	public static final short OPC_is_function_2x_1L = 56;
	public static final short OPC_is_function_2y_1L = 57;
	public static final short OPC_is_boolean_2x_1L = 58;
	public static final short OPC_is_boolean_2y_1L = 59;
	public static final short OPC_is_bitstr_2x_1L = 60;
	public static final short OPC_is_bitstr_2y_1L = 61;
	public static final short OPC_test_arity_2x_3I_1L = 62;
	public static final short OPC_test_arity_2y_3I_1L = 63;
	public static final short OPC_is_eq_exact_1L = 64;
	public static final short OPC_is_ne_exact_1L = 65;
	public static final short OPC_is_eq_1L = 66;
	public static final short OPC_is_ne_1L = 67;
	public static final short OPC_is_lt_1L = 68;
	public static final short OPC_is_ge_1L = 69;
	public static final short OPC_is_function2_2x_1L = 70;
	public static final short OPC_is_function2_2y_1L = 71;
	public static final short OPC_select_val_3L_2JV = 72;
	public static final short OPC_select_tuple_arity_3L_2JA = 73;
	public static final short OPC_call_only_2L = 74;
	public static final short OPC_call_1I_2L = 75;
	public static final short OPC_call_ext_2E = 76;
	public static final short OPC_call_ext_only_2E = 77;
	public static final short OPC_call_ext_last_3I_2E = 78;
	public static final short OPC_apply_1I = 79;
	public static final short OPC_call_fun_1I = 80;
	public static final short OPC_apply_last_2I_1I = 81;
	public static final short OPC_call_last_3I_2L = 82;
	public static final short OPC_make_fun2_2I_1I_3IL = 83;
	public static final short OPC_bif0_1E_2x_3L = 84;
	public static final short OPC_bif0_1E_2x_2nolabel = 85;
	public static final short OPC_bif0_1E_2y_3L = 86;
	public static final short OPC_bif0_1E_2y_2nolabel = 87;
	public static final short OPC_bif1_1G_3x_4L = 88;
	public static final short OPC_bif1_1G_3y_4L = 89;
	public static final short OPC_bif1_1E_3x_4L = 90;
	public static final short OPC_bif1_1E_3y_4L = 91;
	public static final short OPC_bif2_1G_4x_5L = 92;
	public static final short OPC_bif2_1G_4y_5L = 93;
	public static final short OPC_bif2_1E_4x_5L = 94;
	public static final short OPC_bif2_1E_4y_5L = 95;
	public static final short OPC_gc_bif1_1G_3x_4L = 96;
	public static final short OPC_gc_bif1_1G_3y_4L = 97;
	public static final short OPC_gc_bif1_1E_3x_4L = 98;
	public static final short OPC_gc_bif1_1E_3y_4L = 99;
	public static final short OPC_gc_bif2_1G_4x_5L = 100;
	public static final short OPC_gc_bif2_1G_4y_5L = 101;
	public static final short OPC_gc_bif2_1E_4x_5L = 102;
	public static final short OPC_gc_bif2_1E_4y_5L = 103;
	public static final short OPC_loop_rec_2x_1L = 104;
	public static final short OPC_loop_rec_2y_1L = 105;
	public static final short OPC_wait_1L = 106;
	public static final short OPC_loop_rec_end_1L = 107;
	public static final short OPC_wait_timeout_1L = 108;
	public static final short OPC_timeout = 109;
	public static final short OPC_bs_start_match2_2x_4I_5x_1L = 110;
	public static final short OPC_bs_start_match2_2x_4I_5y_1L = 111;
	public static final short OPC_bs_start_match2_2y_4I_5x_1L = 112;
	public static final short OPC_bs_start_match2_2y_4I_5y_1L = 113;
	public static final short OPC_bs_get_utf8_2x_4I_5x_1L = 114;
	public static final short OPC_bs_get_utf8_2x_4I_5y_1L = 115;
	public static final short OPC_bs_get_utf8_2y_4I_5x_1L = 116;
	public static final short OPC_bs_get_utf8_2y_4I_5y_1L = 117;
	public static final short OPC_bs_get_utf16_2x_4I_5x_1L = 118;
	public static final short OPC_bs_get_utf16_2x_4I_5y_1L = 119;
	public static final short OPC_bs_get_utf16_2y_4I_5x_1L = 120;
	public static final short OPC_bs_get_utf16_2y_4I_5y_1L = 121;
	public static final short OPC_bs_get_utf32_2x_4I_5x_1L = 122;
	public static final short OPC_bs_get_utf32_2x_4I_5y_1L = 123;
	public static final short OPC_bs_get_utf32_2y_4I_5x_1L = 124;
	public static final short OPC_bs_get_utf32_2y_4I_5y_1L = 125;
	public static final short OPC_bs_match_string_2x_3c_1L = 126;
	public static final short OPC_bs_match_string_2y_3c_1L = 127;
	public static final short OPC_bs_get_integer2_2x_5I_6I_7x_1L = 128;
	public static final short OPC_bs_get_integer2_2x_5I_6I_7y_1L = 129;
	public static final short OPC_bs_get_integer2_2y_5I_6I_7x_1L = 130;
	public static final short OPC_bs_get_integer2_2y_5I_6I_7y_1L = 131;
	public static final short OPC_bs_get_float2_2x_5I_6I_7x_1L = 132;
	public static final short OPC_bs_get_float2_2x_5I_6I_7y_1L = 133;
	public static final short OPC_bs_get_float2_2y_5I_6I_7x_1L = 134;
	public static final short OPC_bs_get_float2_2y_5I_6I_7y_1L = 135;
	public static final short OPC_bs_get_binary2_2x_6I_7x_1L = 136;
	public static final short OPC_bs_get_binary2_2x_6I_7y_1L = 137;
	public static final short OPC_bs_get_binary2_2y_6I_7x_1L = 138;
	public static final short OPC_bs_get_binary2_2y_6I_7y_1L = 139;
	public static final short OPC_bs_test_tail2_2x_3I_1L = 140;
	public static final short OPC_bs_test_tail2_2y_3I_1L = 141;
	public static final short OPC_bs_test_unit_2x_3I_1L = 142;
	public static final short OPC_bs_test_unit_2y_3I_1L = 143;
	public static final short OPC_bs_skip_utf8_2x_4I_1L = 144;
	public static final short OPC_bs_skip_utf8_2y_4I_1L = 145;
	public static final short OPC_bs_skip_utf16_2x_4I_1L = 146;
	public static final short OPC_bs_skip_utf16_2y_4I_1L = 147;
	public static final short OPC_bs_skip_utf32_2x_4I_1L = 148;
	public static final short OPC_bs_skip_utf32_2y_4I_1L = 149;
	public static final short OPC_bs_skip_bits2_2x_4I_5I_1L = 150;
	public static final short OPC_bs_skip_bits2_2y_4I_5I_1L = 151;
	public static final short OPC_bs_utf8_size_3x = 152;
	public static final short OPC_bs_utf8_size_3y = 153;
	public static final short OPC_bs_utf16_size_3x = 154;
	public static final short OPC_bs_utf16_size_3y = 155;
	public static final short OPC_bs_save2_1x_2I = 156;
	public static final short OPC_bs_save2_1y_2I = 157;
	public static final short OPC_bs_restore2_1x_2I = 158;
	public static final short OPC_bs_restore2_1y_2I = 159;
	public static final short OPC_bs_init_writable = 160;
	public static final short OPC_bs_init2_5I_6x = 161;
	public static final short OPC_bs_init2_5I_6y = 162;
	public static final short OPC_bs_init_bits_5I_6x = 163;
	public static final short OPC_bs_init_bits_5I_6y = 164;
	public static final short OPC_bs_put_string_1c = 165;
	public static final short OPC_bs_put_integer_3I_4I = 166;
	public static final short OPC_bs_put_binary_3I_4I = 167;
	public static final short OPC_bs_put_float_3I_4I = 168;
	public static final short OPC_bs_put_utf8_2I = 169;
	public static final short OPC_bs_put_utf16_2I = 170;
	public static final short OPC_bs_put_utf32_2I = 171;
	public static final short OPC_bs_add_4I_5x_1L = 172;
	public static final short OPC_bs_add_4I_5x_0nolabel = 173;
	public static final short OPC_bs_add_4I_5y_1L = 174;
	public static final short OPC_bs_add_4I_5y_0nolabel = 175;
	public static final short OPC_bs_append_5I_7I_8x = 176;
	public static final short OPC_bs_append_5I_7I_8y = 177;
	public static final short OPC_bs_private_append_3I_5I_6x = 178;
	public static final short OPC_bs_private_append_3I_5I_6y = 179;
	public static final short OPC_bs_context_to_binary_1x = 180;
	public static final short OPC_bs_context_to_binary_1y = 181;
	public static final short OPC_K_catch_2L_1y = 182;
	public static final short OPC_K_try_2L_1y = 183;
	public static final short OPC_catch_end_1y = 184;
	public static final short OPC_try_end_1y = 185;
	public static final short OPC_try_case_1y = 186;
	public static final short OPC_raise = 187;
	public static final short OPC_fmove_1c_2x = 188;
	public static final short OPC_fmove_1c_2y = 189;
	public static final short OPC_fmove_1c_2f = 190;
	public static final short OPC_fmove_1x_2x = 191;
	public static final short OPC_fmove_1x_2y = 192;
	public static final short OPC_fmove_1x_2f = 193;
	public static final short OPC_fmove_1y_2x = 194;
	public static final short OPC_fmove_1y_2y = 195;
	public static final short OPC_fmove_1y_2f = 196;
	public static final short OPC_fmove_1f_2x = 197;
	public static final short OPC_fmove_1f_2y = 198;
	public static final short OPC_fmove_1f_2f = 199;
	public static final short OPC_fconv_1c_2x = 200;
	public static final short OPC_fconv_1c_2y = 201;
	public static final short OPC_fconv_1c_2f = 202;
	public static final short OPC_fconv_1x_2x = 203;
	public static final short OPC_fconv_1x_2y = 204;
	public static final short OPC_fconv_1x_2f = 205;
	public static final short OPC_fconv_1y_2x = 206;
	public static final short OPC_fconv_1y_2y = 207;
	public static final short OPC_fconv_1y_2f = 208;
	public static final short OPC_fconv_1f_2x = 209;
	public static final short OPC_fconv_1f_2y = 210;
	public static final short OPC_fconv_1f_2f = 211;
	public static final short OPC_fadd_2f_3f_4f = 212;
	public static final short OPC_fsub_2f_3f_4f = 213;
	public static final short OPC_fmul_2f_3f_4f = 214;
	public static final short OPC_fdiv_2f_3f_4f = 215;
private static final int OPC_PREFETCH_BASE = 216;
public static final int S_VARIANT_y = 0;
public static final int S_VARIANT_x = 1;
public static final int S_VARIANT_c = 2;
private static final int SIZE_FETCH_S = 3;
private static final int OPC_FETCH_S = OPC_PREFETCH_BASE + 0; // and 3 forth
private static final int OPC_FETCH_S_S = OPC_PREFETCH_BASE + 3; // and 9 forth
public static final short OPC_PREFETCH_MAX = OPC_PREFETCH_BASE + 12;	public static final short MAX_OPCODE = OPC_PREFETCH_MAX;

	public static final short ENSURE_REG_CAPACITY = MAX_OPCODE + 1;

	public static EModule beamFileToEModule(BeamFileData bfd) {
		Encoder encoder = new Encoder();
		bfd.accept(encoder);
		return encoder.toEModule();
	}

	public static class Encoder extends AbstractInterpreter.Encoder {

		protected EModule makeModule(String name,
									 char[] code, EObject[] consts,
									 ValueJumpTable[] value_jump_tables,
									 ArityJumpTable[] arity_jump_tables,
									 List<FunIDWithEntry> exports, List<FunIDWithGuardedness> imports)
		{
			return new Module(name, code, consts,
							  value_jump_tables, arity_jump_tables,
							  exports, imports);
		}

		public FunctionVisitor visitFunction(EAtom name, int arity, int startLabel) {
			return new FunctionEncoder(name, arity, startLabel);
		}

		//--------------------------------------------------

		class FunctionEncoder implements FunctionVisitor, BlockVisitor {
			final EAtom name;
			final int arity;
			final int startLabel;
			private int tuple_pos;

			public FunctionEncoder(EAtom name, int arity, int startLabel) {
				this.name = name;
				this.arity = arity;
				this.startLabel = startLabel;
			}

			/** Common for FunctionVisitor and BlockVisitor... */
			public void visitEnd() {}

			public BlockVisitor visitLabeledBlock(int label) {
				registerLabel(label);
				return this;
			}

			public void visitInsn(Insn insn) {
				int opcode_pos = emitPlaceholder();
				insn_start.put(opcode_pos, insn);

				//System.err.println("@ "+opcode_pos+": "+insn.toSymbolic());
				switch (insn.opcode()) {
case K_return: {
	Insn typed_insn = (Insn) insn;
	emitAt(opcode_pos, OPC_K_return);

} break;
case send: {
	Insn typed_insn = (Insn) insn;
	emitAt(opcode_pos, OPC_send);

} break;
case remove_message: {
	Insn typed_insn = (Insn) insn;
	emitAt(opcode_pos, OPC_remove_message);

} break;
case int_code_end: {
	Insn typed_insn = (Insn) insn;
	nop(opcode_pos);
} break;
case if_end: {
	Insn typed_insn = (Insn) insn;
	emitAt(opcode_pos, OPC_if_end);

} break;
case func_info: {
	Insn.AAI typed_insn = (Insn.AAI) insn;
//DB| $varmap{mod}
	if ((typed_insn.a1) instanceof Operands.Literal) {
		Operands.Literal typed_mod = (Operands.Literal)typed_insn.a1;
		emit(encodeLiteral(typed_mod));
//DB| $varmap{fun}
		if ((typed_insn.a2) instanceof Operands.Literal) {
			Operands.Literal typed_fun = (Operands.Literal)typed_insn.a2;
			emit(encodeLiteral(typed_fun));
//DB| $varmap{arity}
			if (true) {
				int typed_arity = (int)typed_insn.i3;
				emit((char)(typed_arity));
				emitAt(opcode_pos, OPC_func_info_1c_2c_3I);
			} else throw new Error("Unrecognized operand: "+typed_insn.i3);
		} else throw new Error("Unrecognized operand: "+typed_insn.a2);
	} else throw new Error("Unrecognized operand: "+typed_insn.a1);

} break;
case badmatch: {
	Insn.S typed_insn = (Insn.S) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: src
		emitAt(opcode_pos, OPC_badmatch);

} break;
case case_end: {
	Insn.S typed_insn = (Insn.S) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: src
		emitAt(opcode_pos, OPC_case_end);

} break;
case try_case_end: {
	Insn.S typed_insn = (Insn.S) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: src
		emitAt(opcode_pos, OPC_try_case_end);

} break;
case allocate: {
	Insn.I typed_insn = (Insn.I) insn;
//DB| $varmap{slots}
	if (true) {
		int typed_slots = (int)typed_insn.i1;
		emit((char)(typed_slots));
		emitAt(opcode_pos, OPC_allocate_1I);
	} else throw new Error("Unrecognized operand: "+typed_insn.i1);

} break;
case deallocate: {
	Insn.I typed_insn = (Insn.I) insn;
//DB| $varmap{slots}
	if (true) {
		int typed_slots = (int)typed_insn.i1;
		emit((char)(typed_slots));
		emitAt(opcode_pos, OPC_deallocate_1I);
	} else throw new Error("Unrecognized operand: "+typed_insn.i1);

} break;
case allocate_zero: {
	Insn.II typed_insn = (Insn.II) insn;
//DB| $varmap{slots}
	if (true) {
		int typed_slots = (int)typed_insn.i1;
		emit((char)(typed_slots));
		emitAt(opcode_pos, OPC_allocate_zero_1I);
	} else throw new Error("Unrecognized operand: "+typed_insn.i1);

} break;
case allocate_heap: {
	Insn.IWI typed_insn = (Insn.IWI) insn;
//DB| $varmap{stacksize}
	if (true) {
		int typed_stacksize = (int)typed_insn.i1;
		emit((char)(typed_stacksize));
		emitAt(opcode_pos, OPC_allocate_heap_1I);
	} else throw new Error("Unrecognized operand: "+typed_insn.i1);

} break;
case allocate_heap_zero: {
	Insn.IWI typed_insn = (Insn.IWI) insn;
//DB| $varmap{stacksize}
	if (true) {
		int typed_stacksize = (int)typed_insn.i1;
		emit((char)(typed_stacksize));
		emitAt(opcode_pos, OPC_allocate_heap_zero_1I);
	} else throw new Error("Unrecognized operand: "+typed_insn.i1);

} break;
case test_heap: {
	Insn.WI typed_insn = (Insn.WI) insn;
	nop(opcode_pos);
} break;
case init: {
	Insn.D typed_insn = (Insn.D) insn;
//DB| $varmap{dest}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
		emit(typed_dest.nr);
		emitAt(opcode_pos, OPC_init_1x);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
		emit(typed_dest.nr);
		emitAt(opcode_pos, OPC_init_1y);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case trim: {
	Insn.II typed_insn = (Insn.II) insn;
//DB| $varmap{amount}
	if (true) {
		int typed_amount = (int)typed_insn.i1;
		emit((char)(typed_amount));
		emitAt(opcode_pos, OPC_trim_1I);
	} else throw new Error("Unrecognized operand: "+typed_insn.i1);

} break;
case move: {
	Insn.SD typed_insn = (Insn.SD) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: src
//DB| $varmap{dst}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_dst = (Operands.XReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_move_2x);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_dst = (Operands.YReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_move_2y);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case put_list: {
	Insn.SSD typed_insn = (Insn.SSD) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.src1, typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: h
// DB| fetch_arg: t
//DB| $varmap{dst}
			if ((typed_insn.dest) instanceof Operands.XReg) {
				Operands.XReg typed_dst = (Operands.XReg)typed_insn.dest;
				emit(typed_dst.nr);
				emitAt(opcode_pos, OPC_put_list_3x);
			} else if ((typed_insn.dest) instanceof Operands.YReg) {
				Operands.YReg typed_dst = (Operands.YReg)typed_insn.dest;
				emit(typed_dst.nr);
				emitAt(opcode_pos, OPC_put_list_3y);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case get_list: {
	Insn.SDD typed_insn = (Insn.SDD) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: src
//DB| $varmap{h}
		if ((typed_insn.dest1) instanceof Operands.XReg) {
			Operands.XReg typed_h = (Operands.XReg)typed_insn.dest1;
			emit(typed_h.nr);
//DB| $varmap{t}
			if ((typed_insn.dest2) instanceof Operands.XReg) {
				Operands.XReg typed_t = (Operands.XReg)typed_insn.dest2;
				emit(typed_t.nr);
				emitAt(opcode_pos, OPC_get_list_2x_3x);
			} else if ((typed_insn.dest2) instanceof Operands.YReg) {
				Operands.YReg typed_t = (Operands.YReg)typed_insn.dest2;
				emit(typed_t.nr);
				emitAt(opcode_pos, OPC_get_list_2x_3y);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest2);
		} else if ((typed_insn.dest1) instanceof Operands.YReg) {
			Operands.YReg typed_h = (Operands.YReg)typed_insn.dest1;
			emit(typed_h.nr);
//DB| $varmap{t}
			if ((typed_insn.dest2) instanceof Operands.XReg) {
				Operands.XReg typed_t = (Operands.XReg)typed_insn.dest2;
				emit(typed_t.nr);
				emitAt(opcode_pos, OPC_get_list_2y_3x);
			} else if ((typed_insn.dest2) instanceof Operands.YReg) {
				Operands.YReg typed_t = (Operands.YReg)typed_insn.dest2;
				emit(typed_t.nr);
				emitAt(opcode_pos, OPC_get_list_2y_3y);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest2);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest1);

} break;
case get_tuple_element: {
	Insn.SID typed_insn = (Insn.SID) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: src
//DB| $varmap{pos}
		if (true) {
			int typed_pos = (int)typed_insn.i;
			emit((char)(typed_pos));
//DB| $varmap{dst}
			if ((typed_insn.dest) instanceof Operands.XReg) {
				Operands.XReg typed_dst = (Operands.XReg)typed_insn.dest;
				emit(typed_dst.nr);
				emitAt(opcode_pos, OPC_get_tuple_element_2I_3x);
			} else if ((typed_insn.dest) instanceof Operands.YReg) {
				Operands.YReg typed_dst = (Operands.YReg)typed_insn.dest;
				emit(typed_dst.nr);
				emitAt(opcode_pos, OPC_get_tuple_element_2I_3y);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest);
		} else throw new Error("Unrecognized operand: "+typed_insn.i);

} break;
case put_tuple: {
	Insn.ID typed_insn = (Insn.ID) insn;
{tuple_pos=0;}
//DB| $varmap{size}
	if (true) {
		int typed_size = (int)typed_insn.i1;
		emit((char)(typed_size));
//DB| $varmap{dst}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_dst = (Operands.XReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_put_tuple_1I_2x);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_dst = (Operands.YReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_put_tuple_1I_2y);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);
	} else throw new Error("Unrecognized operand: "+typed_insn.i1);

} break;
case put: {
	Insn.S typed_insn = (Insn.S) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
emit(++tuple_pos);
// DB| fetch_arg: src
			emitAt(opcode_pos, OPC_put);

} break;
case set_tuple_element: {
	Insn.SDI typed_insn = (Insn.SDI) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: src
//DB| $varmap{dest}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
			emit(typed_dest.nr);
//DB| $varmap{index}
			if (true) {
				int typed_index = (int)typed_insn.i;
				emit((char)(typed_index));
				emitAt(opcode_pos, OPC_set_tuple_element_2x_3I);
			} else throw new Error("Unrecognized operand: "+typed_insn.i);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
			emit(typed_dest.nr);
//DB| $varmap{index}
			if (true) {
				int typed_index = (int)typed_insn.i;
				emit((char)(typed_index));
				emitAt(opcode_pos, OPC_set_tuple_element_2y_3I);
			} else throw new Error("Unrecognized operand: "+typed_insn.i);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case jump: {
	Insn.L typed_insn = (Insn.L) insn;
//DB| $varmap{lbl}
	if ((typed_insn.label) instanceof Operands.Label) {
		Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
		emit(encodeLabel(typed_lbl.nr));
		emitAt(opcode_pos, OPC_jump_1L);
	} else throw new Error("Unrecognized operand: "+typed_insn.label);

} break;
case is_integer: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_integer_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_integer_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_float: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_float_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_float_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_number: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_number_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_number_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_atom: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_atom_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_atom_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_pid: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_pid_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_pid_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_reference: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_reference_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_reference_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_port: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_port_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_port_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_nil: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_nil_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_nil_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_binary: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_binary_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_binary_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_list: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_list_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_list_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_nonempty_list: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_nonempty_list_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_nonempty_list_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_tuple: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_tuple_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_tuple_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_function: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_function_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_function_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_boolean: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_boolean_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_boolean_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_bitstr: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_bitstr_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_is_bitstr_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case test_arity: {
	Insn.LDI typed_insn = (Insn.LDI) insn;
//DB| $varmap{arg}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_arg = (Operands.XReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{arity}
		if (true) {
			int typed_arity = (int)typed_insn.i;
			emit((char)(typed_arity));
//DB| $varmap{lbl}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_lbl.nr));
				emitAt(opcode_pos, OPC_test_arity_2x_3I_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.i);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_arg = (Operands.YReg)typed_insn.dest;
		emit(typed_arg.nr);
//DB| $varmap{arity}
		if (true) {
			int typed_arity = (int)typed_insn.i;
			emit((char)(typed_arity));
//DB| $varmap{lbl}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_lbl.nr));
				emitAt(opcode_pos, OPC_test_arity_2y_3I_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.i);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case is_eq_exact: {
	Insn.LSS typed_insn = (Insn.LSS) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.src1, typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: a
// DB| fetch_arg: b
//DB| $varmap{lbl}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_lbl.nr));
				emitAt(opcode_pos, OPC_is_eq_exact_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);

} break;
case is_ne_exact: {
	Insn.LSS typed_insn = (Insn.LSS) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.src1, typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: a
// DB| fetch_arg: b
//DB| $varmap{lbl}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_lbl.nr));
				emitAt(opcode_pos, OPC_is_ne_exact_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);

} break;
case is_eq: {
	Insn.LSS typed_insn = (Insn.LSS) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.src1, typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: a
// DB| fetch_arg: b
//DB| $varmap{lbl}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_lbl.nr));
				emitAt(opcode_pos, OPC_is_eq_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);

} break;
case is_ne: {
	Insn.LSS typed_insn = (Insn.LSS) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.src1, typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: a
// DB| fetch_arg: b
//DB| $varmap{lbl}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_lbl.nr));
				emitAt(opcode_pos, OPC_is_ne_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);

} break;
case is_lt: {
	Insn.LSS typed_insn = (Insn.LSS) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.src1, typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: a
// DB| fetch_arg: b
//DB| $varmap{lbl}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_lbl.nr));
				emitAt(opcode_pos, OPC_is_lt_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);

} break;
case is_ge: {
	Insn.LSS typed_insn = (Insn.LSS) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.src1, typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: a
// DB| fetch_arg: b
//DB| $varmap{lbl}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_lbl.nr));
				emitAt(opcode_pos, OPC_is_ge_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);

} break;
case is_function2: {
	Insn.LDS typed_insn = (Insn.LDS) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: arity
//DB| $varmap{subject}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_subject = (Operands.XReg)typed_insn.dest;
			emit(typed_subject.nr);
//DB| $varmap{lbl}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_lbl.nr));
				emitAt(opcode_pos, OPC_is_function2_2x_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_subject = (Operands.YReg)typed_insn.dest;
			emit(typed_subject.nr);
//DB| $varmap{lbl}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_lbl.nr));
				emitAt(opcode_pos, OPC_is_function2_2y_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case select_val: {
	Insn.Select typed_insn = (Insn.Select) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: src
//DB| $varmap{lbl}
		if ((typed_insn.defaultLabel) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.defaultLabel;
			emit(encodeLabel(typed_lbl.nr));
//DB| $varmap{table}
			if ((typed_insn.jumpTable) instanceof Operands.SelectList) {
				Operands.SelectList typed_table = (Operands.SelectList)typed_insn.jumpTable;
				emit(encodeValueJumpTable(typed_table));
				emitAt(opcode_pos, OPC_select_val_3L_2JV);
			} else throw new Error("Unrecognized operand: "+typed_insn.jumpTable);
		} else throw new Error("Unrecognized operand: "+typed_insn.defaultLabel);

} break;
case select_tuple_arity: {
	Insn.Select typed_insn = (Insn.Select) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: src
//DB| $varmap{lbl}
		if ((typed_insn.defaultLabel) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.defaultLabel;
			emit(encodeLabel(typed_lbl.nr));
//DB| $varmap{table}
			if ((typed_insn.jumpTable) instanceof Operands.SelectList) {
				Operands.SelectList typed_table = (Operands.SelectList)typed_insn.jumpTable;
				emit(encodeArityJumpTable(typed_table));
				emitAt(opcode_pos, OPC_select_tuple_arity_3L_2JA);
			} else throw new Error("Unrecognized operand: "+typed_insn.jumpTable);
		} else throw new Error("Unrecognized operand: "+typed_insn.defaultLabel);

} break;
case call_only: {
	Insn.IL typed_insn = (Insn.IL) insn;
//DB| $varmap{lbl}
	if ((typed_insn.label) instanceof Operands.Label) {
		Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
		emit(encodeLabel(typed_lbl.nr));
		emitAt(opcode_pos, OPC_call_only_2L);
	} else throw new Error("Unrecognized operand: "+typed_insn.label);

} break;
case call: {
	Insn.IL typed_insn = (Insn.IL) insn;
//DB| $varmap{keep}
	if (true) {
		int typed_keep = (int)typed_insn.i1;
		emit((char)(typed_keep));
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_call_1I_2L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.i1);

} break;
case call_ext: {
	Insn.IE typed_insn = (Insn.IE) insn;
//DB| $varmap{extfun}
	if ((typed_insn.ext_fun) instanceof ExtFun) {
		ExtFun typed_extfun = (ExtFun)typed_insn.ext_fun;
		emit(encodeExtFun(typed_extfun));
		emitAt(opcode_pos, OPC_call_ext_2E);
	} else throw new Error("Unrecognized operand: "+typed_insn.ext_fun);

} break;
case call_ext_only: {
	Insn.IE typed_insn = (Insn.IE) insn;
//DB| $varmap{extfun}
	if ((typed_insn.ext_fun) instanceof ExtFun) {
		ExtFun typed_extfun = (ExtFun)typed_insn.ext_fun;
		emit(encodeExtFun(typed_extfun));
		emitAt(opcode_pos, OPC_call_ext_only_2E);
	} else throw new Error("Unrecognized operand: "+typed_insn.ext_fun);

} break;
case call_ext_last: {
	Insn.IEI typed_insn = (Insn.IEI) insn;
//DB| $varmap{dealloc}
	if (true) {
		int typed_dealloc = (int)typed_insn.i3;
		emit((char)(typed_dealloc));
//DB| $varmap{extfun}
		if ((typed_insn.ext_fun) instanceof ExtFun) {
			ExtFun typed_extfun = (ExtFun)typed_insn.ext_fun;
			emit(encodeExtFun(typed_extfun));
			emitAt(opcode_pos, OPC_call_ext_last_3I_2E);
		} else throw new Error("Unrecognized operand: "+typed_insn.ext_fun);
	} else throw new Error("Unrecognized operand: "+typed_insn.i3);

} break;
case apply: {
	Insn.I typed_insn = (Insn.I) insn;
//DB| $varmap{arity}
	if (true) {
		int typed_arity = (int)typed_insn.i1;
		emit((char)(typed_arity));
		emitAt(opcode_pos, OPC_apply_1I);
	} else throw new Error("Unrecognized operand: "+typed_insn.i1);

} break;
case call_fun: {
	Insn.I typed_insn = (Insn.I) insn;
//DB| $varmap{arity}
	if (true) {
		int typed_arity = (int)typed_insn.i1;
		emit((char)(typed_arity));
		emitAt(opcode_pos, OPC_call_fun_1I);
	} else throw new Error("Unrecognized operand: "+typed_insn.i1);

} break;
case apply_last: {
	Insn.II typed_insn = (Insn.II) insn;
//DB| $varmap{dealloc}
	if (true) {
		int typed_dealloc = (int)typed_insn.i2;
		emit((char)(typed_dealloc));
//DB| $varmap{arity}
		if (true) {
			int typed_arity = (int)typed_insn.i1;
			emit((char)(typed_arity));
			emitAt(opcode_pos, OPC_apply_last_2I_1I);
		} else throw new Error("Unrecognized operand: "+typed_insn.i1);
	} else throw new Error("Unrecognized operand: "+typed_insn.i2);

} break;
case call_last: {
	Insn.ILI typed_insn = (Insn.ILI) insn;
//DB| $varmap{dealloc}
	if (true) {
		int typed_dealloc = (int)typed_insn.i3;
		emit((char)(typed_dealloc));
//DB| $varmap{lbl}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_lbl.nr));
			emitAt(opcode_pos, OPC_call_last_3I_2L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.i3);

} break;
case make_fun2: {
	Insn.F typed_insn = (Insn.F) insn;
//DB| $varmap{free_vars}
	if (true) {
		int typed_free_vars = (int)typed_insn.anon_fun.free_vars;
		emit((char)(typed_free_vars));
//DB| $varmap{total_arity}
		if (true) {
			int typed_total_arity = (int)typed_insn.anon_fun.total_arity;
			emit((char)(typed_total_arity));
//DB| $varmap{label}
			if (true) {
				int typed_label = (int)typed_insn.anon_fun.label;
				emit(encodeLabel(typed_label));
				emitAt(opcode_pos, OPC_make_fun2_2I_1I_3IL);
			} else throw new Error("Unrecognized operand: "+typed_insn.anon_fun.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.anon_fun.total_arity);
	} else throw new Error("Unrecognized operand: "+typed_insn.anon_fun.free_vars);

} break;
case bif0: {
	Insn.Bif typed_insn = (Insn.Bif) insn;
//DB| $varmap{bif}
	if ((typed_insn.ext_fun) instanceof ExtFun) {
		ExtFun typed_bif = (ExtFun)typed_insn.ext_fun;
		emit(encodeExtFun(typed_bif));
//DB| $varmap{dest}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
			emit(typed_dest.nr);
//DB| $varmap{onFail}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_onFail.nr));
				emitAt(opcode_pos, OPC_bif0_1E_2x_3L);
			} else if (((typed_insn.label) == null || (typed_insn.label).nr == 0)) {
				Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
				emitAt(opcode_pos, OPC_bif0_1E_2x_2nolabel);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
			emit(typed_dest.nr);
//DB| $varmap{onFail}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_onFail.nr));
				emitAt(opcode_pos, OPC_bif0_1E_2y_3L);
			} else if (((typed_insn.label) == null || (typed_insn.label).nr == 0)) {
				Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
				emitAt(opcode_pos, OPC_bif0_1E_2y_2nolabel);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);
	} else throw new Error("Unrecognized operand: "+typed_insn.ext_fun);

} break;
case bif1: {
	Insn.Bif typed_insn = (Insn.Bif) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.args[0]);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: arg1
//DB| $varmap{bif}
		if ((typed_insn.ext_fun) instanceof ExtFun && typed_insn.label != null) {
			ExtFun typed_bif = (ExtFun)typed_insn.ext_fun;
			emit(encodeGuardExtFun(typed_bif));
//DB| $varmap{dest}
			if ((typed_insn.dest) instanceof Operands.XReg) {
				Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
				emit(typed_dest.nr);
//DB| $varmap{onFail}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_onFail.nr));
					emitAt(opcode_pos, OPC_bif1_1G_3x_4L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else if ((typed_insn.dest) instanceof Operands.YReg) {
				Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
				emit(typed_dest.nr);
//DB| $varmap{onFail}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_onFail.nr));
					emitAt(opcode_pos, OPC_bif1_1G_3y_4L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest);
		} else if ((typed_insn.ext_fun) instanceof ExtFun) {
			ExtFun typed_bif = (ExtFun)typed_insn.ext_fun;
			emit(encodeExtFun(typed_bif));
//DB| $varmap{dest}
			if ((typed_insn.dest) instanceof Operands.XReg) {
				Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
				emit(typed_dest.nr);
//DB| $varmap{onFail}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_onFail.nr));
					emitAt(opcode_pos, OPC_bif1_1E_3x_4L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else if ((typed_insn.dest) instanceof Operands.YReg) {
				Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
				emit(typed_dest.nr);
//DB| $varmap{onFail}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_onFail.nr));
					emitAt(opcode_pos, OPC_bif1_1E_3y_4L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest);
		} else throw new Error("Unrecognized operand: "+typed_insn.ext_fun);

} break;
case bif2: {
	Insn.Bif typed_insn = (Insn.Bif) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.args[0], typed_insn.args[1]);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: arg1
// DB| fetch_arg: arg2
//DB| $varmap{bif}
			if ((typed_insn.ext_fun) instanceof ExtFun && typed_insn.label != null) {
				ExtFun typed_bif = (ExtFun)typed_insn.ext_fun;
				emit(encodeGuardExtFun(typed_bif));
//DB| $varmap{dest}
				if ((typed_insn.dest) instanceof Operands.XReg) {
					Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
					emit(typed_dest.nr);
//DB| $varmap{onFail}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_onFail.nr));
						emitAt(opcode_pos, OPC_bif2_1G_4x_5L);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else if ((typed_insn.dest) instanceof Operands.YReg) {
					Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
					emit(typed_dest.nr);
//DB| $varmap{onFail}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_onFail.nr));
						emitAt(opcode_pos, OPC_bif2_1G_4y_5L);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else throw new Error("Unrecognized operand: "+typed_insn.dest);
			} else if ((typed_insn.ext_fun) instanceof ExtFun) {
				ExtFun typed_bif = (ExtFun)typed_insn.ext_fun;
				emit(encodeExtFun(typed_bif));
//DB| $varmap{dest}
				if ((typed_insn.dest) instanceof Operands.XReg) {
					Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
					emit(typed_dest.nr);
//DB| $varmap{onFail}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_onFail.nr));
						emitAt(opcode_pos, OPC_bif2_1E_4x_5L);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else if ((typed_insn.dest) instanceof Operands.YReg) {
					Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
					emit(typed_dest.nr);
//DB| $varmap{onFail}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_onFail.nr));
						emitAt(opcode_pos, OPC_bif2_1E_4y_5L);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else throw new Error("Unrecognized operand: "+typed_insn.dest);
			} else throw new Error("Unrecognized operand: "+typed_insn.ext_fun);

} break;
case gc_bif1: {
	Insn.GcBif typed_insn = (Insn.GcBif) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.args[0]);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: arg1
//DB| $varmap{bif}
		if ((typed_insn.ext_fun) instanceof ExtFun && typed_insn.label != null) {
			ExtFun typed_bif = (ExtFun)typed_insn.ext_fun;
			emit(encodeGuardExtFun(typed_bif));
//DB| $varmap{dest}
			if ((typed_insn.dest) instanceof Operands.XReg) {
				Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
				emit(typed_dest.nr);
//DB| $varmap{onFail}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_onFail.nr));
					emitAt(opcode_pos, OPC_gc_bif1_1G_3x_4L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else if ((typed_insn.dest) instanceof Operands.YReg) {
				Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
				emit(typed_dest.nr);
//DB| $varmap{onFail}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_onFail.nr));
					emitAt(opcode_pos, OPC_gc_bif1_1G_3y_4L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest);
		} else if ((typed_insn.ext_fun) instanceof ExtFun) {
			ExtFun typed_bif = (ExtFun)typed_insn.ext_fun;
			emit(encodeExtFun(typed_bif));
//DB| $varmap{dest}
			if ((typed_insn.dest) instanceof Operands.XReg) {
				Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
				emit(typed_dest.nr);
//DB| $varmap{onFail}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_onFail.nr));
					emitAt(opcode_pos, OPC_gc_bif1_1E_3x_4L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else if ((typed_insn.dest) instanceof Operands.YReg) {
				Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
				emit(typed_dest.nr);
//DB| $varmap{onFail}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_onFail.nr));
					emitAt(opcode_pos, OPC_gc_bif1_1E_3y_4L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest);
		} else throw new Error("Unrecognized operand: "+typed_insn.ext_fun);

} break;
case gc_bif2: {
	Insn.GcBif typed_insn = (Insn.GcBif) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.args[0], typed_insn.args[1]);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: arg1
// DB| fetch_arg: arg2
//DB| $varmap{bif}
			if ((typed_insn.ext_fun) instanceof ExtFun && typed_insn.label != null) {
				ExtFun typed_bif = (ExtFun)typed_insn.ext_fun;
				emit(encodeGuardExtFun(typed_bif));
//DB| $varmap{dest}
				if ((typed_insn.dest) instanceof Operands.XReg) {
					Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
					emit(typed_dest.nr);
//DB| $varmap{onFail}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_onFail.nr));
						emitAt(opcode_pos, OPC_gc_bif2_1G_4x_5L);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else if ((typed_insn.dest) instanceof Operands.YReg) {
					Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
					emit(typed_dest.nr);
//DB| $varmap{onFail}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_onFail.nr));
						emitAt(opcode_pos, OPC_gc_bif2_1G_4y_5L);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else throw new Error("Unrecognized operand: "+typed_insn.dest);
			} else if ((typed_insn.ext_fun) instanceof ExtFun) {
				ExtFun typed_bif = (ExtFun)typed_insn.ext_fun;
				emit(encodeExtFun(typed_bif));
//DB| $varmap{dest}
				if ((typed_insn.dest) instanceof Operands.XReg) {
					Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
					emit(typed_dest.nr);
//DB| $varmap{onFail}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_onFail.nr));
						emitAt(opcode_pos, OPC_gc_bif2_1E_4x_5L);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else if ((typed_insn.dest) instanceof Operands.YReg) {
					Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
					emit(typed_dest.nr);
//DB| $varmap{onFail}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_onFail.nr));
						emitAt(opcode_pos, OPC_gc_bif2_1E_4y_5L);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else throw new Error("Unrecognized operand: "+typed_insn.dest);
			} else throw new Error("Unrecognized operand: "+typed_insn.ext_fun);

} break;
case loop_rec: {
	Insn.LD typed_insn = (Insn.LD) insn;
//DB| $varmap{dest}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
		emit(typed_dest.nr);
//DB| $varmap{label}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_label = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_label.nr));
			emitAt(opcode_pos, OPC_loop_rec_2x_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
		emit(typed_dest.nr);
//DB| $varmap{label}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_label = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_label.nr));
			emitAt(opcode_pos, OPC_loop_rec_2y_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case wait: {
	Insn.L typed_insn = (Insn.L) insn;
//DB| $varmap{label}
	if ((typed_insn.label) instanceof Operands.Label) {
		Operands.Label typed_label = (Operands.Label)typed_insn.label;
		emit(encodeLabel(typed_label.nr));
		emitAt(opcode_pos, OPC_wait_1L);
	} else throw new Error("Unrecognized operand: "+typed_insn.label);

} break;
case loop_rec_end: {
	Insn.L typed_insn = (Insn.L) insn;
//DB| $varmap{label}
	if ((typed_insn.label) instanceof Operands.Label) {
		Operands.Label typed_label = (Operands.Label)typed_insn.label;
		emit(encodeLabel(typed_label.nr));
		emitAt(opcode_pos, OPC_loop_rec_end_1L);
	} else throw new Error("Unrecognized operand: "+typed_insn.label);

} break;
case wait_timeout: {
	Insn.LS typed_insn = (Insn.LS) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: millis
//DB| $varmap{label}
		if ((typed_insn.label) instanceof Operands.Label) {
			Operands.Label typed_label = (Operands.Label)typed_insn.label;
			emit(encodeLabel(typed_label.nr));
			emitAt(opcode_pos, OPC_wait_timeout_1L);
		} else throw new Error("Unrecognized operand: "+typed_insn.label);

} break;
case timeout: {
	Insn typed_insn = (Insn) insn;
	emitAt(opcode_pos, OPC_timeout);

} break;
case recv_mark: {
	Insn.L typed_insn = (Insn.L) insn;
	nop(opcode_pos);
} break;
case recv_set: {
	Insn.L typed_insn = (Insn.L) insn;
	nop(opcode_pos);
} break;
case bs_start_match2: {
	Insn.LDIID typed_insn = (Insn.LDIID) insn;
//DB| $varmap{src}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_src = (Operands.XReg)typed_insn.dest;
		emit(typed_src.nr);
//DB| $varmap{slots}
		if (true) {
			int typed_slots = (int)typed_insn.i4;
			emit((char)(typed_slots));
//DB| $varmap{dest}
			if ((typed_insn.dest5) instanceof Operands.XReg) {
				Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_start_match2_2x_4I_5x_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else if ((typed_insn.dest5) instanceof Operands.YReg) {
				Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_start_match2_2x_4I_5y_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest5);
		} else throw new Error("Unrecognized operand: "+typed_insn.i4);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_src = (Operands.YReg)typed_insn.dest;
		emit(typed_src.nr);
//DB| $varmap{slots}
		if (true) {
			int typed_slots = (int)typed_insn.i4;
			emit((char)(typed_slots));
//DB| $varmap{dest}
			if ((typed_insn.dest5) instanceof Operands.XReg) {
				Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_start_match2_2y_4I_5x_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else if ((typed_insn.dest5) instanceof Operands.YReg) {
				Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_start_match2_2y_4I_5y_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest5);
		} else throw new Error("Unrecognized operand: "+typed_insn.i4);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_get_utf8: {
	Insn.LDIID typed_insn = (Insn.LDIID) insn;
//DB| $varmap{src}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_src = (Operands.XReg)typed_insn.dest;
		emit(typed_src.nr);
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i4;
			emit((char)(typed_flags));
//DB| $varmap{dest}
			if ((typed_insn.dest5) instanceof Operands.XReg) {
				Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_get_utf8_2x_4I_5x_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else if ((typed_insn.dest5) instanceof Operands.YReg) {
				Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_get_utf8_2x_4I_5y_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest5);
		} else throw new Error("Unrecognized operand: "+typed_insn.i4);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_src = (Operands.YReg)typed_insn.dest;
		emit(typed_src.nr);
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i4;
			emit((char)(typed_flags));
//DB| $varmap{dest}
			if ((typed_insn.dest5) instanceof Operands.XReg) {
				Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_get_utf8_2y_4I_5x_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else if ((typed_insn.dest5) instanceof Operands.YReg) {
				Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_get_utf8_2y_4I_5y_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest5);
		} else throw new Error("Unrecognized operand: "+typed_insn.i4);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_get_utf16: {
	Insn.LDIID typed_insn = (Insn.LDIID) insn;
//DB| $varmap{src}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_src = (Operands.XReg)typed_insn.dest;
		emit(typed_src.nr);
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i4;
			emit((char)(typed_flags));
//DB| $varmap{dest}
			if ((typed_insn.dest5) instanceof Operands.XReg) {
				Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_get_utf16_2x_4I_5x_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else if ((typed_insn.dest5) instanceof Operands.YReg) {
				Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_get_utf16_2x_4I_5y_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest5);
		} else throw new Error("Unrecognized operand: "+typed_insn.i4);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_src = (Operands.YReg)typed_insn.dest;
		emit(typed_src.nr);
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i4;
			emit((char)(typed_flags));
//DB| $varmap{dest}
			if ((typed_insn.dest5) instanceof Operands.XReg) {
				Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_get_utf16_2y_4I_5x_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else if ((typed_insn.dest5) instanceof Operands.YReg) {
				Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_get_utf16_2y_4I_5y_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest5);
		} else throw new Error("Unrecognized operand: "+typed_insn.i4);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_get_utf32: {
	Insn.LDIID typed_insn = (Insn.LDIID) insn;
//DB| $varmap{src}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_src = (Operands.XReg)typed_insn.dest;
		emit(typed_src.nr);
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i4;
			emit((char)(typed_flags));
//DB| $varmap{dest}
			if ((typed_insn.dest5) instanceof Operands.XReg) {
				Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_get_utf32_2x_4I_5x_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else if ((typed_insn.dest5) instanceof Operands.YReg) {
				Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_get_utf32_2x_4I_5y_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest5);
		} else throw new Error("Unrecognized operand: "+typed_insn.i4);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_src = (Operands.YReg)typed_insn.dest;
		emit(typed_src.nr);
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i4;
			emit((char)(typed_flags));
//DB| $varmap{dest}
			if ((typed_insn.dest5) instanceof Operands.XReg) {
				Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_get_utf32_2y_4I_5x_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else if ((typed_insn.dest5) instanceof Operands.YReg) {
				Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest5;
				emit(typed_dest.nr);
//DB| $varmap{failLabel}
				if ((typed_insn.label) instanceof Operands.Label) {
					Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
					emit(encodeLabel(typed_failLabel.nr));
					emitAt(opcode_pos, OPC_bs_get_utf32_2y_4I_5y_1L);
				} else throw new Error("Unrecognized operand: "+typed_insn.label);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest5);
		} else throw new Error("Unrecognized operand: "+typed_insn.i4);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_match_string: {
	Insn.LDBi typed_insn = (Insn.LDBi) insn;
//DB| $varmap{src}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_src = (Operands.XReg)typed_insn.dest;
		emit(typed_src.nr);
//DB| $varmap{string}
		if ((typed_insn.bin) instanceof Operands.Literal) {
			Operands.Literal typed_string = (Operands.Literal)typed_insn.bin;
			emit(encodeLiteral(typed_string));
//DB| $varmap{failLabel}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_failLabel.nr));
				emitAt(opcode_pos, OPC_bs_match_string_2x_3c_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.bin);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_src = (Operands.YReg)typed_insn.dest;
		emit(typed_src.nr);
//DB| $varmap{string}
		if ((typed_insn.bin) instanceof Operands.Literal) {
			Operands.Literal typed_string = (Operands.Literal)typed_insn.bin;
			emit(encodeLiteral(typed_string));
//DB| $varmap{failLabel}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_failLabel.nr));
				emitAt(opcode_pos, OPC_bs_match_string_2y_3c_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.bin);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_get_integer2: {
	Insn.LDISIID typed_insn = (Insn.LDISIID) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src4);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: bits
//DB| $varmap{src}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_src = (Operands.XReg)typed_insn.dest;
			emit(typed_src.nr);
//DB| $varmap{unit}
			if (true) {
				int typed_unit = (int)typed_insn.i5;
				emit((char)(typed_unit));
//DB| $varmap{flags}
				if (true) {
					int typed_flags = (int)typed_insn.i6;
					emit((char)(typed_flags));
//DB| $varmap{dest}
					if ((typed_insn.dest7) instanceof Operands.XReg) {
						Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest7;
						emit(typed_dest.nr);
//DB| $varmap{failLabel}
						if ((typed_insn.label) instanceof Operands.Label) {
							Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
							emit(encodeLabel(typed_failLabel.nr));
							emitAt(opcode_pos, OPC_bs_get_integer2_2x_5I_6I_7x_1L);
						} else throw new Error("Unrecognized operand: "+typed_insn.label);
					} else if ((typed_insn.dest7) instanceof Operands.YReg) {
						Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest7;
						emit(typed_dest.nr);
//DB| $varmap{failLabel}
						if ((typed_insn.label) instanceof Operands.Label) {
							Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
							emit(encodeLabel(typed_failLabel.nr));
							emitAt(opcode_pos, OPC_bs_get_integer2_2x_5I_6I_7y_1L);
						} else throw new Error("Unrecognized operand: "+typed_insn.label);
					} else throw new Error("Unrecognized operand: "+typed_insn.dest7);
				} else throw new Error("Unrecognized operand: "+typed_insn.i6);
			} else throw new Error("Unrecognized operand: "+typed_insn.i5);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_src = (Operands.YReg)typed_insn.dest;
			emit(typed_src.nr);
//DB| $varmap{unit}
			if (true) {
				int typed_unit = (int)typed_insn.i5;
				emit((char)(typed_unit));
//DB| $varmap{flags}
				if (true) {
					int typed_flags = (int)typed_insn.i6;
					emit((char)(typed_flags));
//DB| $varmap{dest}
					if ((typed_insn.dest7) instanceof Operands.XReg) {
						Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest7;
						emit(typed_dest.nr);
//DB| $varmap{failLabel}
						if ((typed_insn.label) instanceof Operands.Label) {
							Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
							emit(encodeLabel(typed_failLabel.nr));
							emitAt(opcode_pos, OPC_bs_get_integer2_2y_5I_6I_7x_1L);
						} else throw new Error("Unrecognized operand: "+typed_insn.label);
					} else if ((typed_insn.dest7) instanceof Operands.YReg) {
						Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest7;
						emit(typed_dest.nr);
//DB| $varmap{failLabel}
						if ((typed_insn.label) instanceof Operands.Label) {
							Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
							emit(encodeLabel(typed_failLabel.nr));
							emitAt(opcode_pos, OPC_bs_get_integer2_2y_5I_6I_7y_1L);
						} else throw new Error("Unrecognized operand: "+typed_insn.label);
					} else throw new Error("Unrecognized operand: "+typed_insn.dest7);
				} else throw new Error("Unrecognized operand: "+typed_insn.i6);
			} else throw new Error("Unrecognized operand: "+typed_insn.i5);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_get_float2: {
	Insn.LDISIID typed_insn = (Insn.LDISIID) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src4);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: bits
//DB| $varmap{src}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_src = (Operands.XReg)typed_insn.dest;
			emit(typed_src.nr);
//DB| $varmap{unit}
			if (true) {
				int typed_unit = (int)typed_insn.i5;
				emit((char)(typed_unit));
//DB| $varmap{flags}
				if (true) {
					int typed_flags = (int)typed_insn.i6;
					emit((char)(typed_flags));
//DB| $varmap{dest}
					if ((typed_insn.dest7) instanceof Operands.XReg) {
						Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest7;
						emit(typed_dest.nr);
//DB| $varmap{failLabel}
						if ((typed_insn.label) instanceof Operands.Label) {
							Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
							emit(encodeLabel(typed_failLabel.nr));
							emitAt(opcode_pos, OPC_bs_get_float2_2x_5I_6I_7x_1L);
						} else throw new Error("Unrecognized operand: "+typed_insn.label);
					} else if ((typed_insn.dest7) instanceof Operands.YReg) {
						Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest7;
						emit(typed_dest.nr);
//DB| $varmap{failLabel}
						if ((typed_insn.label) instanceof Operands.Label) {
							Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
							emit(encodeLabel(typed_failLabel.nr));
							emitAt(opcode_pos, OPC_bs_get_float2_2x_5I_6I_7y_1L);
						} else throw new Error("Unrecognized operand: "+typed_insn.label);
					} else throw new Error("Unrecognized operand: "+typed_insn.dest7);
				} else throw new Error("Unrecognized operand: "+typed_insn.i6);
			} else throw new Error("Unrecognized operand: "+typed_insn.i5);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_src = (Operands.YReg)typed_insn.dest;
			emit(typed_src.nr);
//DB| $varmap{unit}
			if (true) {
				int typed_unit = (int)typed_insn.i5;
				emit((char)(typed_unit));
//DB| $varmap{flags}
				if (true) {
					int typed_flags = (int)typed_insn.i6;
					emit((char)(typed_flags));
//DB| $varmap{dest}
					if ((typed_insn.dest7) instanceof Operands.XReg) {
						Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest7;
						emit(typed_dest.nr);
//DB| $varmap{failLabel}
						if ((typed_insn.label) instanceof Operands.Label) {
							Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
							emit(encodeLabel(typed_failLabel.nr));
							emitAt(opcode_pos, OPC_bs_get_float2_2y_5I_6I_7x_1L);
						} else throw new Error("Unrecognized operand: "+typed_insn.label);
					} else if ((typed_insn.dest7) instanceof Operands.YReg) {
						Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest7;
						emit(typed_dest.nr);
//DB| $varmap{failLabel}
						if ((typed_insn.label) instanceof Operands.Label) {
							Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
							emit(encodeLabel(typed_failLabel.nr));
							emitAt(opcode_pos, OPC_bs_get_float2_2y_5I_6I_7y_1L);
						} else throw new Error("Unrecognized operand: "+typed_insn.label);
					} else throw new Error("Unrecognized operand: "+typed_insn.dest7);
				} else throw new Error("Unrecognized operand: "+typed_insn.i6);
			} else throw new Error("Unrecognized operand: "+typed_insn.i5);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_get_binary2: {
	Insn.LDISIID typed_insn = (Insn.LDISIID) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src4);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: bits
//DB| $varmap{ms}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_ms = (Operands.XReg)typed_insn.dest;
			emit(typed_ms.nr);
//DB| $varmap{flags}
			if (true) {
				int typed_flags = (int)typed_insn.i6;
				emit((char)(typed_flags));
//DB| $varmap{dest}
				if ((typed_insn.dest7) instanceof Operands.XReg) {
					Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest7;
					emit(typed_dest.nr);
//DB| $varmap{failLabel}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_failLabel.nr));
						emitAt(opcode_pos, OPC_bs_get_binary2_2x_6I_7x_1L);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else if ((typed_insn.dest7) instanceof Operands.YReg) {
					Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest7;
					emit(typed_dest.nr);
//DB| $varmap{failLabel}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_failLabel.nr));
						emitAt(opcode_pos, OPC_bs_get_binary2_2x_6I_7y_1L);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else throw new Error("Unrecognized operand: "+typed_insn.dest7);
			} else throw new Error("Unrecognized operand: "+typed_insn.i6);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_ms = (Operands.YReg)typed_insn.dest;
			emit(typed_ms.nr);
//DB| $varmap{flags}
			if (true) {
				int typed_flags = (int)typed_insn.i6;
				emit((char)(typed_flags));
//DB| $varmap{dest}
				if ((typed_insn.dest7) instanceof Operands.XReg) {
					Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest7;
					emit(typed_dest.nr);
//DB| $varmap{failLabel}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_failLabel.nr));
						emitAt(opcode_pos, OPC_bs_get_binary2_2y_6I_7x_1L);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else if ((typed_insn.dest7) instanceof Operands.YReg) {
					Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest7;
					emit(typed_dest.nr);
//DB| $varmap{failLabel}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_failLabel.nr));
						emitAt(opcode_pos, OPC_bs_get_binary2_2y_6I_7y_1L);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else throw new Error("Unrecognized operand: "+typed_insn.dest7);
			} else throw new Error("Unrecognized operand: "+typed_insn.i6);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_test_tail2: {
	Insn.LDI typed_insn = (Insn.LDI) insn;
//DB| $varmap{ms}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_ms = (Operands.XReg)typed_insn.dest;
		emit(typed_ms.nr);
//DB| $varmap{bits_left}
		if (true) {
			int typed_bits_left = (int)typed_insn.i;
			emit((char)(typed_bits_left));
//DB| $varmap{failLabel}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_failLabel.nr));
				emitAt(opcode_pos, OPC_bs_test_tail2_2x_3I_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.i);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_ms = (Operands.YReg)typed_insn.dest;
		emit(typed_ms.nr);
//DB| $varmap{bits_left}
		if (true) {
			int typed_bits_left = (int)typed_insn.i;
			emit((char)(typed_bits_left));
//DB| $varmap{failLabel}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_failLabel.nr));
				emitAt(opcode_pos, OPC_bs_test_tail2_2y_3I_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.i);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_test_unit: {
	Insn.LDI typed_insn = (Insn.LDI) insn;
//DB| $varmap{ms}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_ms = (Operands.XReg)typed_insn.dest;
		emit(typed_ms.nr);
//DB| $varmap{unit}
		if (true) {
			int typed_unit = (int)typed_insn.i;
			emit((char)(typed_unit));
//DB| $varmap{failLabel}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_failLabel.nr));
				emitAt(opcode_pos, OPC_bs_test_unit_2x_3I_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.i);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_ms = (Operands.YReg)typed_insn.dest;
		emit(typed_ms.nr);
//DB| $varmap{unit}
		if (true) {
			int typed_unit = (int)typed_insn.i;
			emit((char)(typed_unit));
//DB| $varmap{failLabel}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_failLabel.nr));
				emitAt(opcode_pos, OPC_bs_test_unit_2y_3I_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.i);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_skip_utf8: {
	Insn.LDII typed_insn = (Insn.LDII) insn;
//DB| $varmap{ms}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_ms = (Operands.XReg)typed_insn.dest;
		emit(typed_ms.nr);
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i4;
			emit((char)(typed_flags));
//DB| $varmap{failLabel}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_failLabel.nr));
				emitAt(opcode_pos, OPC_bs_skip_utf8_2x_4I_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.i4);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_ms = (Operands.YReg)typed_insn.dest;
		emit(typed_ms.nr);
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i4;
			emit((char)(typed_flags));
//DB| $varmap{failLabel}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_failLabel.nr));
				emitAt(opcode_pos, OPC_bs_skip_utf8_2y_4I_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.i4);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_skip_utf16: {
	Insn.LDII typed_insn = (Insn.LDII) insn;
//DB| $varmap{ms}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_ms = (Operands.XReg)typed_insn.dest;
		emit(typed_ms.nr);
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i4;
			emit((char)(typed_flags));
//DB| $varmap{failLabel}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_failLabel.nr));
				emitAt(opcode_pos, OPC_bs_skip_utf16_2x_4I_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.i4);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_ms = (Operands.YReg)typed_insn.dest;
		emit(typed_ms.nr);
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i4;
			emit((char)(typed_flags));
//DB| $varmap{failLabel}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_failLabel.nr));
				emitAt(opcode_pos, OPC_bs_skip_utf16_2y_4I_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.i4);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_skip_utf32: {
	Insn.LDII typed_insn = (Insn.LDII) insn;
//DB| $varmap{ms}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_ms = (Operands.XReg)typed_insn.dest;
		emit(typed_ms.nr);
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i4;
			emit((char)(typed_flags));
//DB| $varmap{failLabel}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_failLabel.nr));
				emitAt(opcode_pos, OPC_bs_skip_utf32_2x_4I_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.i4);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_ms = (Operands.YReg)typed_insn.dest;
		emit(typed_ms.nr);
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i4;
			emit((char)(typed_flags));
//DB| $varmap{failLabel}
			if ((typed_insn.label) instanceof Operands.Label) {
				Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
				emit(encodeLabel(typed_failLabel.nr));
				emitAt(opcode_pos, OPC_bs_skip_utf32_2y_4I_1L);
			} else throw new Error("Unrecognized operand: "+typed_insn.label);
		} else throw new Error("Unrecognized operand: "+typed_insn.i4);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_skip_bits2: {
	Insn.LDSII typed_insn = (Insn.LDSII) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src3);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: bits
//DB| $varmap{ms}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_ms = (Operands.XReg)typed_insn.dest;
			emit(typed_ms.nr);
//DB| $varmap{unit}
			if (true) {
				int typed_unit = (int)typed_insn.i4;
				emit((char)(typed_unit));
//DB| $varmap{flags}
				if (true) {
					int typed_flags = (int)typed_insn.i5;
					emit((char)(typed_flags));
//DB| $varmap{failLabel}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_failLabel.nr));
						emitAt(opcode_pos, OPC_bs_skip_bits2_2x_4I_5I_1L);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else throw new Error("Unrecognized operand: "+typed_insn.i5);
			} else throw new Error("Unrecognized operand: "+typed_insn.i4);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_ms = (Operands.YReg)typed_insn.dest;
			emit(typed_ms.nr);
//DB| $varmap{unit}
			if (true) {
				int typed_unit = (int)typed_insn.i4;
				emit((char)(typed_unit));
//DB| $varmap{flags}
				if (true) {
					int typed_flags = (int)typed_insn.i5;
					emit((char)(typed_flags));
//DB| $varmap{failLabel}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_failLabel = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_failLabel.nr));
						emitAt(opcode_pos, OPC_bs_skip_bits2_2y_4I_5I_1L);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else throw new Error("Unrecognized operand: "+typed_insn.i5);
			} else throw new Error("Unrecognized operand: "+typed_insn.i4);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_utf8_size: {
	Insn.LSD typed_insn = (Insn.LSD) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: value
//DB| $varmap{dest}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
			emit(typed_dest.nr);
			emitAt(opcode_pos, OPC_bs_utf8_size_3x);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
			emit(typed_dest.nr);
			emitAt(opcode_pos, OPC_bs_utf8_size_3y);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_utf16_size: {
	Insn.LSD typed_insn = (Insn.LSD) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: value
//DB| $varmap{dest}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
			emit(typed_dest.nr);
			emitAt(opcode_pos, OPC_bs_utf16_size_3x);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
			emit(typed_dest.nr);
			emitAt(opcode_pos, OPC_bs_utf16_size_3y);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_save2: {
	Insn.DI typed_insn = (Insn.DI) insn;
//DB| $varmap{ms}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_ms = (Operands.XReg)typed_insn.dest;
		emit(typed_ms.nr);
//DB| $varmap{pos}
		if (true) {
			int typed_pos = (int)typed_insn.i2;
			emit((char)(typed_pos));
			emitAt(opcode_pos, OPC_bs_save2_1x_2I);
		} else throw new Error("Unrecognized operand: "+typed_insn.i2);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_ms = (Operands.YReg)typed_insn.dest;
		emit(typed_ms.nr);
//DB| $varmap{pos}
		if (true) {
			int typed_pos = (int)typed_insn.i2;
			emit((char)(typed_pos));
			emitAt(opcode_pos, OPC_bs_save2_1y_2I);
		} else throw new Error("Unrecognized operand: "+typed_insn.i2);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_restore2: {
	Insn.DI typed_insn = (Insn.DI) insn;
//DB| $varmap{ms}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_ms = (Operands.XReg)typed_insn.dest;
		emit(typed_ms.nr);
//DB| $varmap{pos}
		if (true) {
			int typed_pos = (int)typed_insn.i2;
			emit((char)(typed_pos));
			emitAt(opcode_pos, OPC_bs_restore2_1x_2I);
		} else throw new Error("Unrecognized operand: "+typed_insn.i2);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_ms = (Operands.YReg)typed_insn.dest;
		emit(typed_ms.nr);
//DB| $varmap{pos}
		if (true) {
			int typed_pos = (int)typed_insn.i2;
			emit((char)(typed_pos));
			emitAt(opcode_pos, OPC_bs_restore2_1y_2I);
		} else throw new Error("Unrecognized operand: "+typed_insn.i2);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case bs_init_writable: {
	Insn typed_insn = (Insn) insn;
	emitAt(opcode_pos, OPC_bs_init_writable);

} break;
case bs_init2: {
	Insn.LSIIID typed_insn = (Insn.LSIIID) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: size
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i5;
			emit((char)(typed_flags));
//DB| $varmap{dest}
			if ((typed_insn.dest) instanceof Operands.XReg) {
				Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
				emit(typed_dest.nr);
				emitAt(opcode_pos, OPC_bs_init2_5I_6x);
			} else if ((typed_insn.dest) instanceof Operands.YReg) {
				Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
				emit(typed_dest.nr);
				emitAt(opcode_pos, OPC_bs_init2_5I_6y);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest);
		} else throw new Error("Unrecognized operand: "+typed_insn.i5);

} break;
case bs_init_bits: {
	Insn.LSIIID typed_insn = (Insn.LSIIID) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: size
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i5;
			emit((char)(typed_flags));
//DB| $varmap{dest}
			if ((typed_insn.dest) instanceof Operands.XReg) {
				Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
				emit(typed_dest.nr);
				emitAt(opcode_pos, OPC_bs_init_bits_5I_6x);
			} else if ((typed_insn.dest) instanceof Operands.YReg) {
				Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
				emit(typed_dest.nr);
				emitAt(opcode_pos, OPC_bs_init_bits_5I_6y);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest);
		} else throw new Error("Unrecognized operand: "+typed_insn.i5);

} break;
case bs_put_string: {
	Insn.By typed_insn = (Insn.By) insn;
//DB| $varmap{value}
	if ((typed_insn.bin) instanceof Operands.Literal) {
		Operands.Literal typed_value = (Operands.Literal)typed_insn.bin;
		emit(encodeLiteral(typed_value));
		emitAt(opcode_pos, OPC_bs_put_string_1c);
	} else throw new Error("Unrecognized operand: "+typed_insn.bin);

} break;
case bs_put_integer: {
	Insn.LSIIS typed_insn = (Insn.LSIIS) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.src5, typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: value
// DB| fetch_arg: size
//DB| $varmap{unit}
			if (true) {
				int typed_unit = (int)typed_insn.i3;
				emit((char)(typed_unit));
//DB| $varmap{flags}
				if (true) {
					int typed_flags = (int)typed_insn.i4;
					emit((char)(typed_flags));
					emitAt(opcode_pos, OPC_bs_put_integer_3I_4I);
				} else throw new Error("Unrecognized operand: "+typed_insn.i4);
			} else throw new Error("Unrecognized operand: "+typed_insn.i3);

} break;
case bs_put_binary: {
	Insn.LSIIS typed_insn = (Insn.LSIIS) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.src2, typed_insn.src5);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: size
// DB| fetch_arg: value
//DB| $varmap{unit}
			if (true) {
				int typed_unit = (int)typed_insn.i3;
				emit((char)(typed_unit));
//DB| $varmap{flags}
				if (true) {
					int typed_flags = (int)typed_insn.i4;
					emit((char)(typed_flags));
					emitAt(opcode_pos, OPC_bs_put_binary_3I_4I);
				} else throw new Error("Unrecognized operand: "+typed_insn.i4);
			} else throw new Error("Unrecognized operand: "+typed_insn.i3);

} break;
case bs_put_float: {
	Insn.LSIIS typed_insn = (Insn.LSIIS) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.src5, typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: value
// DB| fetch_arg: size
//DB| $varmap{unit}
			if (true) {
				int typed_unit = (int)typed_insn.i3;
				emit((char)(typed_unit));
//DB| $varmap{flags}
				if (true) {
					int typed_flags = (int)typed_insn.i4;
					emit((char)(typed_flags));
					emitAt(opcode_pos, OPC_bs_put_float_3I_4I);
				} else throw new Error("Unrecognized operand: "+typed_insn.i4);
			} else throw new Error("Unrecognized operand: "+typed_insn.i3);

} break;
case bs_put_utf8: {
	Insn.LIS typed_insn = (Insn.LIS) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: value
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i2;
			emit((char)(typed_flags));
			emitAt(opcode_pos, OPC_bs_put_utf8_2I);
		} else throw new Error("Unrecognized operand: "+typed_insn.i2);

} break;
case bs_put_utf16: {
	Insn.LIS typed_insn = (Insn.LIS) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: value
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i2;
			emit((char)(typed_flags));
			emitAt(opcode_pos, OPC_bs_put_utf16_2I);
		} else throw new Error("Unrecognized operand: "+typed_insn.i2);

} break;
case bs_put_utf32: {
	Insn.LIS typed_insn = (Insn.LIS) insn;
int prefetch_opcode = encodePrefetch_S(typed_insn.src);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: value
//DB| $varmap{flags}
		if (true) {
			int typed_flags = (int)typed_insn.i2;
			emit((char)(typed_flags));
			emitAt(opcode_pos, OPC_bs_put_utf32_2I);
		} else throw new Error("Unrecognized operand: "+typed_insn.i2);

} break;
case bs_add: {
	Insn.LSSID typed_insn = (Insn.LSSID) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.src1, typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: x
// DB| fetch_arg: y
//DB| $varmap{yunit}
			if (true) {
				int typed_yunit = (int)typed_insn.i3;
				emit((char)(typed_yunit));
//DB| $varmap{dest}
				if ((typed_insn.dest) instanceof Operands.XReg) {
					Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
					emit(typed_dest.nr);
//DB| $varmap{onFail}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_onFail.nr));
						emitAt(opcode_pos, OPC_bs_add_4I_5x_1L);
					} else if (((typed_insn.label) == null || (typed_insn.label).nr == 0)) {
						Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
						emitAt(opcode_pos, OPC_bs_add_4I_5x_0nolabel);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else if ((typed_insn.dest) instanceof Operands.YReg) {
					Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
					emit(typed_dest.nr);
//DB| $varmap{onFail}
					if ((typed_insn.label) instanceof Operands.Label) {
						Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
						emit(encodeLabel(typed_onFail.nr));
						emitAt(opcode_pos, OPC_bs_add_4I_5y_1L);
					} else if (((typed_insn.label) == null || (typed_insn.label).nr == 0)) {
						Operands.Label typed_onFail = (Operands.Label)typed_insn.label;
						emitAt(opcode_pos, OPC_bs_add_4I_5y_0nolabel);
					} else throw new Error("Unrecognized operand: "+typed_insn.label);
				} else throw new Error("Unrecognized operand: "+typed_insn.dest);
			} else throw new Error("Unrecognized operand: "+typed_insn.i3);

} break;
case bs_append: {
	Insn.BSAppend typed_insn = (Insn.BSAppend) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.src6, typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: src
// DB| fetch_arg: extra_size
//DB| $varmap{unit}
			if (true) {
				int typed_unit = (int)typed_insn.i5;
				emit((char)(typed_unit));
//DB| $varmap{flags}
				if (true) {
					int typed_flags = (int)typed_insn.i7;
					emit((char)(typed_flags));
//DB| $varmap{dest}
					if ((typed_insn.dest8) instanceof Operands.XReg) {
						Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest8;
						emit(typed_dest.nr);
						emitAt(opcode_pos, OPC_bs_append_5I_7I_8x);
					} else if ((typed_insn.dest8) instanceof Operands.YReg) {
						Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest8;
						emit(typed_dest.nr);
						emitAt(opcode_pos, OPC_bs_append_5I_7I_8y);
					} else throw new Error("Unrecognized operand: "+typed_insn.dest8);
				} else throw new Error("Unrecognized operand: "+typed_insn.i7);
			} else throw new Error("Unrecognized operand: "+typed_insn.i5);

} break;
case bs_private_append: {
	Insn.BSPrivateAppend typed_insn = (Insn.BSPrivateAppend) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.src4, typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: src
// DB| fetch_arg: extra_size
//DB| $varmap{unit}
			if (true) {
				int typed_unit = (int)typed_insn.i3;
				emit((char)(typed_unit));
//DB| $varmap{flags}
				if (true) {
					int typed_flags = (int)typed_insn.i5;
					emit((char)(typed_flags));
//DB| $varmap{dest}
					if ((typed_insn.dest) instanceof Operands.XReg) {
						Operands.XReg typed_dest = (Operands.XReg)typed_insn.dest;
						emit(typed_dest.nr);
						emitAt(opcode_pos, OPC_bs_private_append_3I_5I_6x);
					} else if ((typed_insn.dest) instanceof Operands.YReg) {
						Operands.YReg typed_dest = (Operands.YReg)typed_insn.dest;
						emit(typed_dest.nr);
						emitAt(opcode_pos, OPC_bs_private_append_3I_5I_6y);
					} else throw new Error("Unrecognized operand: "+typed_insn.dest);
				} else throw new Error("Unrecognized operand: "+typed_insn.i5);
			} else throw new Error("Unrecognized operand: "+typed_insn.i3);

} break;
case bs_context_to_binary: {
	Insn.D typed_insn = (Insn.D) insn;
//DB| $varmap{srcdest}
	if ((typed_insn.dest) instanceof Operands.XReg) {
		Operands.XReg typed_srcdest = (Operands.XReg)typed_insn.dest;
		emit(typed_srcdest.nr);
		emitAt(opcode_pos, OPC_bs_context_to_binary_1x);
	} else if ((typed_insn.dest) instanceof Operands.YReg) {
		Operands.YReg typed_srcdest = (Operands.YReg)typed_insn.dest;
		emit(typed_srcdest.nr);
		emitAt(opcode_pos, OPC_bs_context_to_binary_1y);
	} else throw new Error("Unrecognized operand: "+typed_insn.dest);

} break;
case K_catch: {
	Insn.YL typed_insn = (Insn.YL) insn;
//DB| $varmap{lbl}
	if ((typed_insn.label) instanceof Operands.Label) {
		Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
		emit(encodeLabel(typed_lbl.nr));
//DB| $varmap{y}
		if ((typed_insn.y) instanceof Operands.YReg) {
			Operands.YReg typed_y = (Operands.YReg)typed_insn.y;
			emit(typed_y.nr);
			emitAt(opcode_pos, OPC_K_catch_2L_1y);
		} else throw new Error("Unrecognized operand: "+typed_insn.y);
	} else throw new Error("Unrecognized operand: "+typed_insn.label);

} break;
case K_try: {
	Insn.YL typed_insn = (Insn.YL) insn;
//DB| $varmap{lbl}
	if ((typed_insn.label) instanceof Operands.Label) {
		Operands.Label typed_lbl = (Operands.Label)typed_insn.label;
		emit(encodeLabel(typed_lbl.nr));
//DB| $varmap{y}
		if ((typed_insn.y) instanceof Operands.YReg) {
			Operands.YReg typed_y = (Operands.YReg)typed_insn.y;
			emit(typed_y.nr);
			emitAt(opcode_pos, OPC_K_try_2L_1y);
		} else throw new Error("Unrecognized operand: "+typed_insn.y);
	} else throw new Error("Unrecognized operand: "+typed_insn.label);

} break;
case catch_end: {
	Insn.Y typed_insn = (Insn.Y) insn;
//DB| $varmap{y}
	if ((typed_insn.y) instanceof Operands.YReg) {
		Operands.YReg typed_y = (Operands.YReg)typed_insn.y;
		emit(typed_y.nr);
		emitAt(opcode_pos, OPC_catch_end_1y);
	} else throw new Error("Unrecognized operand: "+typed_insn.y);

} break;
case try_end: {
	Insn.Y typed_insn = (Insn.Y) insn;
//DB| $varmap{y}
	if ((typed_insn.y) instanceof Operands.YReg) {
		Operands.YReg typed_y = (Operands.YReg)typed_insn.y;
		emit(typed_y.nr);
		emitAt(opcode_pos, OPC_try_end_1y);
	} else throw new Error("Unrecognized operand: "+typed_insn.y);

} break;
case try_case: {
	Insn.Y typed_insn = (Insn.Y) insn;
//DB| $varmap{y}
	if ((typed_insn.y) instanceof Operands.YReg) {
		Operands.YReg typed_y = (Operands.YReg)typed_insn.y;
		emit(typed_y.nr);
		emitAt(opcode_pos, OPC_try_case_1y);
	} else throw new Error("Unrecognized operand: "+typed_insn.y);

} break;
case raise: {
	Insn.SS typed_insn = (Insn.SS) insn;
int prefetch_opcode = encodePrefetch_S_S(typed_insn.src1, typed_insn.src2);
emitAt(opcode_pos, prefetch_opcode);
opcode_pos = emitPlaceholder();
// DB| fetch_arg: value
// DB| fetch_arg: trace
			emitAt(opcode_pos, OPC_raise);

} break;
case fclearerror: {
	Insn typed_insn = (Insn) insn;
	nop(opcode_pos);
} break;
case fcheckerror: {
	Insn.L typed_insn = (Insn.L) insn;
	nop(opcode_pos);
} break;
case fmove: {
	Insn.SD typed_insn = (Insn.SD) insn;
//DB| $varmap{src}
	if ((typed_insn.src) instanceof Operands.Literal) {
		Operands.Literal typed_src = (Operands.Literal)typed_insn.src;
		emit(encodeLiteral(typed_src));
//DB| $varmap{dst}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_dst = (Operands.XReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fmove_1c_2x);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_dst = (Operands.YReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fmove_1c_2y);
		} else if ((typed_insn.dest) instanceof Operands.FReg) {
			Operands.FReg typed_dst = (Operands.FReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fmove_1c_2f);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);
	} else if ((typed_insn.src) instanceof Operands.XReg) {
		Operands.XReg typed_src = (Operands.XReg)typed_insn.src;
		emit(typed_src.nr);
//DB| $varmap{dst}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_dst = (Operands.XReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fmove_1x_2x);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_dst = (Operands.YReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fmove_1x_2y);
		} else if ((typed_insn.dest) instanceof Operands.FReg) {
			Operands.FReg typed_dst = (Operands.FReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fmove_1x_2f);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);
	} else if ((typed_insn.src) instanceof Operands.YReg) {
		Operands.YReg typed_src = (Operands.YReg)typed_insn.src;
		emit(typed_src.nr);
//DB| $varmap{dst}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_dst = (Operands.XReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fmove_1y_2x);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_dst = (Operands.YReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fmove_1y_2y);
		} else if ((typed_insn.dest) instanceof Operands.FReg) {
			Operands.FReg typed_dst = (Operands.FReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fmove_1y_2f);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);
	} else if ((typed_insn.src) instanceof Operands.FReg) {
		Operands.FReg typed_src = (Operands.FReg)typed_insn.src;
		emit(typed_src.nr);
//DB| $varmap{dst}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_dst = (Operands.XReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fmove_1f_2x);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_dst = (Operands.YReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fmove_1f_2y);
		} else if ((typed_insn.dest) instanceof Operands.FReg) {
			Operands.FReg typed_dst = (Operands.FReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fmove_1f_2f);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);
	} else throw new Error("Unrecognized operand: "+typed_insn.src);

} break;
case fconv: {
	Insn.SD typed_insn = (Insn.SD) insn;
//DB| $varmap{src}
	if ((typed_insn.src) instanceof Operands.Literal) {
		Operands.Literal typed_src = (Operands.Literal)typed_insn.src;
		emit(encodeLiteral(typed_src));
//DB| $varmap{dst}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_dst = (Operands.XReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fconv_1c_2x);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_dst = (Operands.YReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fconv_1c_2y);
		} else if ((typed_insn.dest) instanceof Operands.FReg) {
			Operands.FReg typed_dst = (Operands.FReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fconv_1c_2f);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);
	} else if ((typed_insn.src) instanceof Operands.XReg) {
		Operands.XReg typed_src = (Operands.XReg)typed_insn.src;
		emit(typed_src.nr);
//DB| $varmap{dst}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_dst = (Operands.XReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fconv_1x_2x);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_dst = (Operands.YReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fconv_1x_2y);
		} else if ((typed_insn.dest) instanceof Operands.FReg) {
			Operands.FReg typed_dst = (Operands.FReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fconv_1x_2f);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);
	} else if ((typed_insn.src) instanceof Operands.YReg) {
		Operands.YReg typed_src = (Operands.YReg)typed_insn.src;
		emit(typed_src.nr);
//DB| $varmap{dst}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_dst = (Operands.XReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fconv_1y_2x);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_dst = (Operands.YReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fconv_1y_2y);
		} else if ((typed_insn.dest) instanceof Operands.FReg) {
			Operands.FReg typed_dst = (Operands.FReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fconv_1y_2f);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);
	} else if ((typed_insn.src) instanceof Operands.FReg) {
		Operands.FReg typed_src = (Operands.FReg)typed_insn.src;
		emit(typed_src.nr);
//DB| $varmap{dst}
		if ((typed_insn.dest) instanceof Operands.XReg) {
			Operands.XReg typed_dst = (Operands.XReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fconv_1f_2x);
		} else if ((typed_insn.dest) instanceof Operands.YReg) {
			Operands.YReg typed_dst = (Operands.YReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fconv_1f_2y);
		} else if ((typed_insn.dest) instanceof Operands.FReg) {
			Operands.FReg typed_dst = (Operands.FReg)typed_insn.dest;
			emit(typed_dst.nr);
			emitAt(opcode_pos, OPC_fconv_1f_2f);
		} else throw new Error("Unrecognized operand: "+typed_insn.dest);
	} else throw new Error("Unrecognized operand: "+typed_insn.src);

} break;
case fadd: {
	Insn.LSSD typed_insn = (Insn.LSSD) insn;
//DB| $varmap{a}
	if ((typed_insn.src1) instanceof Operands.FReg) {
		Operands.FReg typed_a = (Operands.FReg)typed_insn.src1;
		emit(typed_a.nr);
//DB| $varmap{b}
		if ((typed_insn.src2) instanceof Operands.FReg) {
			Operands.FReg typed_b = (Operands.FReg)typed_insn.src2;
			emit(typed_b.nr);
//DB| $varmap{dst}
			if ((typed_insn.dest) instanceof Operands.FReg) {
				Operands.FReg typed_dst = (Operands.FReg)typed_insn.dest;
				emit(typed_dst.nr);
				emitAt(opcode_pos, OPC_fadd_2f_3f_4f);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest);
		} else throw new Error("Unrecognized operand: "+typed_insn.src2);
	} else throw new Error("Unrecognized operand: "+typed_insn.src1);

} break;
case fsub: {
	Insn.LSSD typed_insn = (Insn.LSSD) insn;
//DB| $varmap{a}
	if ((typed_insn.src1) instanceof Operands.FReg) {
		Operands.FReg typed_a = (Operands.FReg)typed_insn.src1;
		emit(typed_a.nr);
//DB| $varmap{b}
		if ((typed_insn.src2) instanceof Operands.FReg) {
			Operands.FReg typed_b = (Operands.FReg)typed_insn.src2;
			emit(typed_b.nr);
//DB| $varmap{dst}
			if ((typed_insn.dest) instanceof Operands.FReg) {
				Operands.FReg typed_dst = (Operands.FReg)typed_insn.dest;
				emit(typed_dst.nr);
				emitAt(opcode_pos, OPC_fsub_2f_3f_4f);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest);
		} else throw new Error("Unrecognized operand: "+typed_insn.src2);
	} else throw new Error("Unrecognized operand: "+typed_insn.src1);

} break;
case fmul: {
	Insn.LSSD typed_insn = (Insn.LSSD) insn;
//DB| $varmap{a}
	if ((typed_insn.src1) instanceof Operands.FReg) {
		Operands.FReg typed_a = (Operands.FReg)typed_insn.src1;
		emit(typed_a.nr);
//DB| $varmap{b}
		if ((typed_insn.src2) instanceof Operands.FReg) {
			Operands.FReg typed_b = (Operands.FReg)typed_insn.src2;
			emit(typed_b.nr);
//DB| $varmap{dst}
			if ((typed_insn.dest) instanceof Operands.FReg) {
				Operands.FReg typed_dst = (Operands.FReg)typed_insn.dest;
				emit(typed_dst.nr);
				emitAt(opcode_pos, OPC_fmul_2f_3f_4f);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest);
		} else throw new Error("Unrecognized operand: "+typed_insn.src2);
	} else throw new Error("Unrecognized operand: "+typed_insn.src1);

} break;
case fdiv: {
	Insn.LSSD typed_insn = (Insn.LSSD) insn;
//DB| $varmap{a}
	if ((typed_insn.src1) instanceof Operands.FReg) {
		Operands.FReg typed_a = (Operands.FReg)typed_insn.src1;
		emit(typed_a.nr);
//DB| $varmap{b}
		if ((typed_insn.src2) instanceof Operands.FReg) {
			Operands.FReg typed_b = (Operands.FReg)typed_insn.src2;
			emit(typed_b.nr);
//DB| $varmap{dst}
			if ((typed_insn.dest) instanceof Operands.FReg) {
				Operands.FReg typed_dst = (Operands.FReg)typed_insn.dest;
				emit(typed_dst.nr);
				emitAt(opcode_pos, OPC_fdiv_2f_3f_4f);
			} else throw new Error("Unrecognized operand: "+typed_insn.dest);
		} else throw new Error("Unrecognized operand: "+typed_insn.src2);
	} else throw new Error("Unrecognized operand: "+typed_insn.src1);

} break;

				default: throw new Error("Unknown opcode: "+insn);
				} // switch
			}
		}

//---------- Prefetch-related declarations:
private int encodePolymorphParameter_S(Operands.SourceOperand v) {
	if ((v) instanceof Operands.Literal) {
		Operands.Literal w = (Operands.Literal)v;
		emit(encodeLiteral(w));
		return S_VARIANT_c;
	} else if ((v) instanceof Operands.XReg) {
		Operands.XReg w = (Operands.XReg)v;
		emit(w.nr);
		return S_VARIANT_x;
	} else if ((v) instanceof Operands.YReg) {
		Operands.YReg w = (Operands.YReg)v;
		emit(w.nr);
		return S_VARIANT_y;
	} else throw new Error("Unrecognized operand: "+v);
}

private int encodePrefetch_S(Operands.SourceOperand v1) {
	int x1 = encodePolymorphParameter_S(v1);
	return OPC_FETCH_S + (0 * SIZE_FETCH_S + x1);
}

private int encodePrefetch_S_S(Operands.SourceOperand v1, Operands.SourceOperand v2) {
	int x1 = encodePolymorphParameter_S(v1);
	int x2 = encodePolymorphParameter_S(v2);
	return OPC_FETCH_S_S + ((0 * SIZE_FETCH_S + x1) * SIZE_FETCH_S + x2);
}

	}

    public static class Module extends AbstractInterpreter.Module {
		final private char[] code;
		final private EObject[] consts;
		final private ValueJumpTable[] value_jump_tables;
		final private ArityJumpTable[] arity_jump_tables;
		final private List<FunIDWithEntry> exports;
		final private List<FunIDWithGuardedness> imports;
		final private EFun[] ext_funs;

		Module(String name,
			   char[] code, EObject[] consts,
			   ValueJumpTable[] value_jump_tables,
			   ArityJumpTable[] arity_jump_tables,
			   List<FunIDWithEntry> exports, List<FunIDWithGuardedness> imports)
		{
			super(name, true);
			this.code = code;
			this.consts = consts;
			this.value_jump_tables = value_jump_tables;
			this.arity_jump_tables = arity_jump_tables;
			this.exports = exports;
			this.imports = imports;
			ext_funs = new EFun[imports.size()];
			if (DEBUG) System.err.println("INT| Constructed module for "+this.name);
			setup();
		}

		/**
		 * This method is used by EModuleManager in function resolution.
		 */
		public void registerImportsAndExports() throws Exception {
			for (int i=0; i<imports.size(); i++) {
				FunIDWithGuardedness imp_wg = imports.get(i);
				FunID imp = imp_wg.fun;
				boolean is_guard = imp_wg.is_guard;
// 				System.err.println("INT| Import #"+i+": "+imp+" / "+is_guard);

				// If this is a BIF, resolve it right away:
				BuiltInFunction bif =
					BIFUtil.getMethod(imp.module.getName(),
									  imp.function.getName(),
									  imp.arity,
									  is_guard, false);

				if (bif != null && java.lang.reflect.Modifier.isStatic(bif.javaMethod.getModifiers())) {
					//TODO: make this work for virtual methods as well
					ext_funs[i] = EFun.make(bif.javaMethod, this.module_name());
				} else {
					EModuleManager.add_import(imp, new VectorFunBinder(ext_funs, imp, i));
				}
			}

			int j=0;
			for (FunIDWithEntry fi : exports) {
// 				System.err.println("INT| Export #"+(j++)+": "+fi);
				EFun fun = EFun.get_fun_with_handler(fi.arity, new Function(fi.start_pc), getModuleClassLoader());
				EModuleManager.add_export(this, fi, fun);
			}

			load_native_bifs();
		}

		class Function implements EFunHandler {
			final int start_pc;

			public Function(int start_pc) {
				this.start_pc = start_pc;
			}

			public EObject invoke(final EProc proc, final EObject[] args) throws Pausable {
				int argCnt = args.length;
				EObject[] reg = getRegs(proc); //??
				for (int i=0; i<argCnt; i++) {reg[i] = args[i];} //??
				//for (int i=0; i<argCnt; i++) System.err.println("INT| arg#"+i+"="+args[i]);
				return interpret(proc, start_pc, reg);
			}

			public EObject invoke(final EProc proc, final EObject[] args, int off, int len) throws Pausable {
				EObject[] reg = getRegs(proc); //??
				if (reg == args) {
					return interpret(proc, start_pc, reg);
				} else {
					int argCnt = len;
					for (int i=0; i<argCnt; i++) {reg[i] = args[i];} //??
					//for (int i=0; i<argCnt; i++) System.err.println("INT| arg#"+i+"="+args[i]);
					return interpret(proc, start_pc, reg);					
				}
			}

			/** Local call - with given PC and register array */
			public EObject invoke_local(final EProc proc, final EObject[] reg, int argCnt, int pc) throws Pausable {
// 				System.err.println("INT| invoking "+name+"@"+pc+"...");
				return interpret(proc, pc, reg);
			}

			public EObject interpret(final EProc proc, int pc, EObject[] reg) throws Pausable {
				final char[] code = Module.this.code;
				EObject stack[] = proc.stack;
				int sp = proc.sp;
				EDouble[] freg = proc.fregs;

				// For exception handling:
				ExceptionHandlerStackElement exh = null;

				// For tuple construction:
				ETuple curtuple = null;

				// For bitstring construction:
				EBitStringBuilder bit_string_builder = null;

				EObject prefetched1 = null, prefetched2 = null;
				int last_pc = pc;

				while (true) try {
						while (true) {
// 							System.err.print('¤');
							if (pc >= code.length) {
								throw new Error("Bad jump to: "+module_name()+"@"+pc+"; from_pc="+last_pc+"; op="+((int)code[last_pc]));
							}
							last_pc = pc;
							final int opcode = code[pc++];
							//System.err.print("STACK| "); for (int i=0; i<=sp; i++) {System.err.print("  "+i+":"+stack[i]);} System.err.println();
// 							System.err.println("INTP|"+proc.self_handle()+" (pc="+(pc-1)+"; sp="+sp+")"+opcode+"   "+reg[0]);
							switch (opcode) {
							case ENSURE_REG_CAPACITY: {
								int max_x = code[pc++];
								System.err.println("INTP|"+proc.self_handle()+" Ensure reg capacity: "+max_x);
								reg = ensureCapacity(reg, max_x);
							} break;












	case OPC_K_return: if (true) {
		{proc.stack=stack; proc.sp=sp; stack=null;}; return reg[0];
	} break;
	case OPC_send: if (true) {
		ERT.send(proc, reg[0], reg[1]);
	} break;
	case OPC_remove_message: if (true) {
		ERT.remove_message(proc);
	} break;
	case OPC_if_end: if (true) {
		return ERT.if_end();
	} break;
	case OPC_func_info_1c_2c_3I: if (true) {
		int _mod = code[pc++];
		int _fun = code[pc++];
		int _arity = code[pc++];
		{{proc.stack=stack; proc.sp=sp; stack=null;}; return ERT.func_info((EAtom)consts[_mod], (EAtom)consts[_fun], xregsSeq(reg,(short) (_arity)));}
	} break;
	case OPC_badmatch: if (true) {
		/*{proc.stack=stack; proc.sp=sp; stack=null;};*/ return ERT.badmatch(prefetched1);
	} break;
	case OPC_case_end: if (true) {
		/*{proc.stack=stack; proc.sp=sp; stack=null;};*/ return ERT.case_end(prefetched1);
	} break;
	case OPC_try_case_end: if (true) {
		/*{proc.stack=stack; proc.sp=sp; stack=null;};*/ return ERT.try_case_end(prefetched1);
	} break;
	case OPC_allocate_1I: if (true) {
		int _slots = code[pc++];
		{stack = ensureCapacity(stack, sp+(short) (_slots), sp+1); sp += (short) (_slots);};
	} break;
	case OPC_deallocate_1I: if (true) {
		int _slots = code[pc++];
		{for (int _i=0; _i<(short) (_slots); _i++) stack[sp--] = null;};
	} break;
	case OPC_allocate_zero_1I: if (true) {
		int _slots = code[pc++];
		{stack = ensureCapacity(stack, sp+(short) (_slots), sp+1); sp += (short) (_slots);};
	} break;
	case OPC_allocate_heap_1I: if (true) {
		int _stacksize = code[pc++];
		{stack = ensureCapacity(stack, sp+(short) (_stacksize), sp+1); sp += (short) (_stacksize);};
	} break;
	case OPC_allocate_heap_zero_1I: if (true) {
		int _stacksize = code[pc++];
		{stack = ensureCapacity(stack, sp+(short) (_stacksize), sp+1); sp += (short) (_stacksize);};
	} break;
	case OPC_init_1x: if (true) {
		int _dest = code[pc++];
		reg[_dest] = (null);
	} break;
	case OPC_init_1y: if (true) {
		int _dest = code[pc++];
		stack[sp - (_dest)] = (null);
	} break;
	case OPC_trim_1I: if (true) {
		int _amount = code[pc++];
		{for (int _i=0; _i<(short) (_amount); _i++) stack[sp--] = null;};
	} break;
	case OPC_move_2x: if (true) {
		int _dst = code[pc++];
		reg[_dst] = (prefetched1);
	} break;
	case OPC_move_2y: if (true) {
		int _dst = code[pc++];
		stack[sp - (_dst)] = (prefetched1);
	} break;
	case OPC_put_list_3x: if (true) {
		int _dst = code[pc++];
		reg[_dst] = (ERT.cons(prefetched1, prefetched2));
	} break;
	case OPC_put_list_3y: if (true) {
		int _dst = code[pc++];
		stack[sp - (_dst)] = (ERT.cons(prefetched1, prefetched2));
	} break;
	case OPC_get_list_2x_3x: if (true) {
		int _h = code[pc++];
		int _t = code[pc++];
		{ECons cons = prefetched1.testNonEmptyList(); reg[_h] = (cons.head()); reg[_t] = (cons.tail());}
	} break;
	case OPC_get_list_2x_3y: if (true) {
		int _h = code[pc++];
		int _t = code[pc++];
		{ECons cons = prefetched1.testNonEmptyList(); reg[_h] = (cons.head()); stack[sp - (_t)] = (cons.tail());}
	} break;
	case OPC_get_list_2y_3x: if (true) {
		int _h = code[pc++];
		int _t = code[pc++];
		{ECons cons = prefetched1.testNonEmptyList(); stack[sp - (_h)] = (cons.head()); reg[_t] = (cons.tail());}
	} break;
	case OPC_get_list_2y_3y: if (true) {
		int _h = code[pc++];
		int _t = code[pc++];
		{ECons cons = prefetched1.testNonEmptyList(); stack[sp - (_h)] = (cons.head()); stack[sp - (_t)] = (cons.tail());}
	} break;
	case OPC_get_tuple_element_2I_3x: if (true) {
		int _pos = code[pc++];
		int _dst = code[pc++];
		reg[_dst] = (((ETuple)prefetched1).elm(1+(short) (_pos)));
	} break;
	case OPC_get_tuple_element_2I_3y: if (true) {
		int _pos = code[pc++];
		int _dst = code[pc++];
		stack[sp - (_dst)] = (((ETuple)prefetched1).elm(1+(short) (_pos)));
	} break;
	case OPC_put_tuple_1I_2x: if (true) {
		int _size = code[pc++];
		int _dst = code[pc++];
		reg[_dst] = (curtuple = ETuple.make((short) (_size)));
	} break;
	case OPC_put_tuple_1I_2y: if (true) {
		int _size = code[pc++];
		int _dst = code[pc++];
		stack[sp - (_dst)] = (curtuple = ETuple.make((short) (_size)));
	} break;
	case OPC_put: if (true) {
		int _index = code[pc++];
		curtuple.set(_index, prefetched1);
	} break;
	case OPC_set_tuple_element_2x_3I: if (true) {
		int _dest = code[pc++];
		int _index = code[pc++];
		((ETuple)reg[_dest]).set((short) (_index)+1, prefetched1);
	} break;
	case OPC_set_tuple_element_2y_3I: if (true) {
		int _dest = code[pc++];
		int _index = code[pc++];
		((ETuple)stack[sp - (_dest)]).set((short) (_index)+1, prefetched1);
	} break;
	case OPC_jump_1L: if (true) {
		int _lbl = code[pc++];
		pc = (_lbl);
	} break;
	case OPC_is_integer_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testInteger() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_integer_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testInteger() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_float_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testFloat()   	   == null) pc = (_lbl);
	} break;
	case OPC_is_float_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testFloat()   	   == null) pc = (_lbl);
	} break;
	case OPC_is_number_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testNumber()  	   == null) pc = (_lbl);
	} break;
	case OPC_is_number_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testNumber()  	   == null) pc = (_lbl);
	} break;
	case OPC_is_atom_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testAtom()    	   == null) pc = (_lbl);
	} break;
	case OPC_is_atom_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testAtom()    	   == null) pc = (_lbl);
	} break;
	case OPC_is_pid_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testPID() 		   == null) pc = (_lbl);
	} break;
	case OPC_is_pid_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testPID() 		   == null) pc = (_lbl);
	} break;
	case OPC_is_reference_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testReference() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_reference_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testReference() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_port_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testPort() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_port_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testPort() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_nil_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testNil() 		   == null) pc = (_lbl);
	} break;
	case OPC_is_nil_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testNil() 		   == null) pc = (_lbl);
	} break;
	case OPC_is_binary_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testBinary() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_binary_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testBinary() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_list_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testCons() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_list_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testCons() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_nonempty_list_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testNonEmptyList()    == null) pc = (_lbl);
	} break;
	case OPC_is_nonempty_list_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testNonEmptyList()    == null) pc = (_lbl);
	} break;
	case OPC_is_tuple_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testTuple() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_tuple_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testTuple() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_function_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testFunction() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_function_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testFunction() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_boolean_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testBoolean() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_boolean_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testBoolean() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_bitstr_2x_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testBitString() 	   == null) pc = (_lbl);
	} break;
	case OPC_is_bitstr_2y_1L: if (true) {
		int _arg = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testBitString() 	   == null) pc = (_lbl);
	} break;
	case OPC_test_arity_2x_3I_1L: if (true) {
		int _arg = code[pc++];
		int _arity = code[pc++];
		int _lbl = code[pc++];
		if (reg[_arg].testTuple() == null || ((ETuple)reg[_arg]).arity() != (short) (_arity)) pc = (_lbl);
	} break;
	case OPC_test_arity_2y_3I_1L: if (true) {
		int _arg = code[pc++];
		int _arity = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_arg)].testTuple() == null || ((ETuple)stack[sp - (_arg)]).arity() != (short) (_arity)) pc = (_lbl);
	} break;
	case OPC_is_eq_exact_1L: if (true) {
		int _lbl = code[pc++];
		if (! prefetched1.equalsExactly(prefetched2)) pc = (_lbl);
	} break;
	case OPC_is_ne_exact_1L: if (true) {
		int _lbl = code[pc++];
		if (prefetched1.equalsExactly(prefetched2)) pc = (_lbl);
	} break;
	case OPC_is_eq_1L: if (true) {
		int _lbl = code[pc++];
		if (prefetched1.erlangCompareTo(prefetched2) != 0) pc = (_lbl);
	} break;
	case OPC_is_ne_1L: if (true) {
		int _lbl = code[pc++];
		if (prefetched1.erlangCompareTo(prefetched2) == 0) pc = (_lbl);
	} break;
	case OPC_is_lt_1L: if (true) {
		int _lbl = code[pc++];
		if (prefetched1.erlangCompareTo(prefetched2) >= 0) pc = (_lbl);
	} break;
	case OPC_is_ge_1L: if (true) {
		int _lbl = code[pc++];
		if (prefetched1.erlangCompareTo(prefetched2) < 0) pc = (_lbl);
	} break;
	case OPC_is_function2_2x_1L: if (true) {
		int _subject = code[pc++];
		int _lbl = code[pc++];
		if (reg[_subject].testFunction2(prefetched1.asInt()) == null) pc = (_lbl);
	} break;
	case OPC_is_function2_2y_1L: if (true) {
		int _subject = code[pc++];
		int _lbl = code[pc++];
		if (stack[sp - (_subject)].testFunction2(prefetched1.asInt()) == null) pc = (_lbl);
	} break;
	case OPC_select_val_3L_2JV: if (true) {
		int _lbl = code[pc++];
		int _table = code[pc++];
		pc = value_jump_tables[_table].lookup(prefetched1, (_lbl));
	} break;
	case OPC_select_tuple_arity_3L_2JA: if (true) {
		int _lbl = code[pc++];
		int _table = code[pc++];
		{ETuple tuple_val = prefetched1.testTuple(); if (tuple_val == null) pc = (_lbl); else {int arity=tuple_val.arity(); pc = arity_jump_tables[_table].lookup(arity, (_lbl));}}
	} break;
	case OPC_call_only_2L: if (true) {
		int _lbl = code[pc++];
		pc = (_lbl);
	} break;
	case OPC_call_1I_2L: if (true) {
		int _keep = code[pc++];
		int _lbl = code[pc++];
		{proc.stack=stack; proc.sp=sp; stack=null;}; reg[0] = invoke_local(proc, reg, (short) (_keep), (_lbl)); {stack=proc.stack; assert(proc.sp==sp);};
	} break;
	case OPC_call_ext_2E: if (true) {
		int _extfun = code[pc++];
		{proc.stack=stack; proc.sp=sp; stack=null;}; reg[0] = ext_funs[_extfun].invoke(proc, reg, 0, ext_funs[_extfun].arity()); {stack=proc.stack; assert(proc.sp==sp);};
	} break;
	case OPC_call_ext_only_2E: if (true) {
		int _extfun = code[pc++];
		{proc.stack=stack; proc.sp=sp; stack=null;}; return ext_funs[_extfun].invoke(proc, reg, 0, ext_funs[_extfun].arity());
	} break;
	case OPC_call_ext_last_3I_2E: if (true) {
		int _dealloc = code[pc++];
		int _extfun = code[pc++];
		{for (int _i=0; _i<(short) (_dealloc); _i++) stack[sp--] = null;}; {proc.stack=stack; proc.sp=sp; stack=null;}; return ext_funs[_extfun].invoke(proc, reg, 0, ext_funs[_extfun].arity());
	} break;
	case OPC_apply_1I: if (true) {
		int _arity = code[pc++];
		{proc.stack=stack; proc.sp=sp; stack=null;}; int ary=(short) (_arity); reg[0] = ERT.resolve_fun(reg[ary], reg[ary+1], ary).invoke(proc, reg, 0, ary); {stack=proc.stack; assert(proc.sp==sp);};
	} break;
	case OPC_call_fun_1I: if (true) {
		int _arity = code[pc++];
		{proc.stack=stack; proc.sp=sp; stack=null;}; int ary=(short) (_arity); reg[0] = ((EFun)reg[ary]).invoke(proc, reg, 0, ary); {stack=proc.stack; assert(proc.sp==sp);};
	} break;
	case OPC_apply_last_2I_1I: if (true) {
		int _dealloc = code[pc++];
		int _arity = code[pc++];
		{for (int _i=0; _i<(short) (_dealloc); _i++) stack[sp--] = null;}; int ary = (short) (_arity); EFun fun = ERT.resolve_fun(reg[ary], reg[ary+1], ary); {proc.stack=stack; proc.sp=sp; stack=null;}; return fun.invoke(proc, xregsArray(reg,ary));
	} break;
	case OPC_call_last_3I_2L: if (true) {
		int _dealloc = code[pc++];
		int _lbl = code[pc++];
		{for (int _i=0; _i<(short) (_dealloc); _i++) stack[sp--] = null;}; pc = (_lbl);
	} break;
	case OPC_make_fun2_2I_1I_3IL: if (true) {
		int _free_vars = code[pc++];
		int _total_arity = code[pc++];
		int _label = code[pc++];
		reg[0] = (EFun.get_fun_with_handler( (short) (_total_arity)-(short) (_free_vars), new Closure(xregsArray(reg,(short) (_free_vars)),  (_label)), getModuleClassLoader()));
	} break;
	case OPC_bif0_1E_2x_3L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{}); if (true && tmp==null) pc = (_onFail); reg[_dest] = (tmp);}
	} break;
	case OPC_bif0_1E_2x_2nolabel: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{}); if (true && tmp==null) pc = nofailLabel(); reg[_dest] = (tmp);}
	} break;
	case OPC_bif0_1E_2y_3L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{}); if (true && tmp==null) pc = (_onFail); stack[sp - (_dest)] = (tmp);}
	} break;
	case OPC_bif0_1E_2y_2nolabel: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{}); if (true && tmp==null) pc = nofailLabel(); stack[sp - (_dest)] = (tmp);}
	} break;
	case OPC_bif1_1G_3x_4L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1}); if (true && tmp==null) pc = (_onFail); reg[_dest] = (tmp);}
	} break;
	case OPC_bif1_1G_3y_4L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1}); if (true && tmp==null) pc = (_onFail); stack[sp - (_dest)] = (tmp);}
	} break;
	case OPC_bif1_1E_3x_4L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1}); if (true && tmp==null) pc = (_onFail); reg[_dest] = (tmp);}
	} break;
	case OPC_bif1_1E_3y_4L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1}); if (true && tmp==null) pc = (_onFail); stack[sp - (_dest)] = (tmp);}
	} break;
	case OPC_bif2_1G_4x_5L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1, prefetched2}); if (true && tmp==null) pc = (_onFail); reg[_dest] = (tmp);}
	} break;
	case OPC_bif2_1G_4y_5L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1, prefetched2}); if (true && tmp==null) pc = (_onFail); stack[sp - (_dest)] = (tmp);}
	} break;
	case OPC_bif2_1E_4x_5L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1, prefetched2}); if (true && tmp==null) pc = (_onFail); reg[_dest] = (tmp);}
	} break;
	case OPC_bif2_1E_4y_5L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1, prefetched2}); if (true && tmp==null) pc = (_onFail); stack[sp - (_dest)] = (tmp);}
	} break;
	case OPC_gc_bif1_1G_3x_4L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1}); if (tmp==null) pc = (_onFail); reg[_dest] = (tmp);}
	} break;
	case OPC_gc_bif1_1G_3y_4L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1}); if (tmp==null) pc = (_onFail); stack[sp - (_dest)] = (tmp);}
	} break;
	case OPC_gc_bif1_1E_3x_4L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1}); if (tmp==null) pc = (_onFail); reg[_dest] = (tmp);}
	} break;
	case OPC_gc_bif1_1E_3y_4L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1}); if (tmp==null) pc = (_onFail); stack[sp - (_dest)] = (tmp);}
	} break;
	case OPC_gc_bif2_1G_4x_5L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1, prefetched2}); if (tmp==null) pc = (_onFail); reg[_dest] = (tmp);}
	} break;
	case OPC_gc_bif2_1G_4y_5L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1, prefetched2}); if (tmp==null) pc = (_onFail); stack[sp - (_dest)] = (tmp);}
	} break;
	case OPC_gc_bif2_1E_4x_5L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1, prefetched2}); if (tmp==null) pc = (_onFail); reg[_dest] = (tmp);}
	} break;
	case OPC_gc_bif2_1E_4y_5L: if (true) {
		int _bif = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		{EObject tmp = ext_funs[_bif].invoke(proc, new EObject[]{prefetched1, prefetched2}); if (tmp==null) pc = (_onFail); stack[sp - (_dest)] = (tmp);}
	} break;
	case OPC_loop_rec_2x_1L: if (true) {
		int _dest = code[pc++];
		int _label = code[pc++];
		EObject tmp = ERT.loop_rec(proc); if (tmp==null) pc = (_label); else reg[_dest] = (tmp);
	} break;
	case OPC_loop_rec_2y_1L: if (true) {
		int _dest = code[pc++];
		int _label = code[pc++];
		EObject tmp = ERT.loop_rec(proc); if (tmp==null) pc = (_label); else stack[sp - (_dest)] = (tmp);
	} break;
	case OPC_wait_1L: if (true) {
		int _label = code[pc++];
		ERT.wait(proc); pc = (_label);
	} break;
	case OPC_loop_rec_end_1L: if (true) {
		int _label = code[pc++];
		ERT.loop_rec_end(proc); pc = (_label);
	} break;
	case OPC_wait_timeout_1L: if (true) {
		int _label = code[pc++];
		if (ERT.wait_timeout(proc, prefetched1)) pc = (_label);
	} break;
	case OPC_timeout: if (true) {
		ERT.timeout(proc);
	} break;
	case OPC_bs_start_match2_2x_4I_5x_1L: if (true) {
		int _src = code[pc++];
		int _slots = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = EBinMatchState.bs_start_match2(reg[_src], (short) (_slots)); if (tmp==null) pc = (_failLabel); else reg[_dest] = (tmp);
	} break;
	case OPC_bs_start_match2_2x_4I_5y_1L: if (true) {
		int _src = code[pc++];
		int _slots = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = EBinMatchState.bs_start_match2(reg[_src], (short) (_slots)); if (tmp==null) pc = (_failLabel); else stack[sp - (_dest)] = (tmp);
	} break;
	case OPC_bs_start_match2_2y_4I_5x_1L: if (true) {
		int _src = code[pc++];
		int _slots = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = EBinMatchState.bs_start_match2(stack[sp - (_src)], (short) (_slots)); if (tmp==null) pc = (_failLabel); else reg[_dest] = (tmp);
	} break;
	case OPC_bs_start_match2_2y_4I_5y_1L: if (true) {
		int _src = code[pc++];
		int _slots = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = EBinMatchState.bs_start_match2(stack[sp - (_src)], (short) (_slots)); if (tmp==null) pc = (_failLabel); else stack[sp - (_dest)] = (tmp);
	} break;
	case OPC_bs_get_utf8_2x_4I_5x_1L: if (true) {
		int _src = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		int chr = ((EBinMatchState)(reg[_src])).bs_get_utf8((short) (_flags)); if (chr < 0) pc = (_failLabel); else reg[_dest] = (ERT.box(chr));
	} break;
	case OPC_bs_get_utf8_2x_4I_5y_1L: if (true) {
		int _src = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		int chr = ((EBinMatchState)(reg[_src])).bs_get_utf8((short) (_flags)); if (chr < 0) pc = (_failLabel); else stack[sp - (_dest)] = (ERT.box(chr));
	} break;
	case OPC_bs_get_utf8_2y_4I_5x_1L: if (true) {
		int _src = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		int chr = ((EBinMatchState)(stack[sp - (_src)])).bs_get_utf8((short) (_flags)); if (chr < 0) pc = (_failLabel); else reg[_dest] = (ERT.box(chr));
	} break;
	case OPC_bs_get_utf8_2y_4I_5y_1L: if (true) {
		int _src = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		int chr = ((EBinMatchState)(stack[sp - (_src)])).bs_get_utf8((short) (_flags)); if (chr < 0) pc = (_failLabel); else stack[sp - (_dest)] = (ERT.box(chr));
	} break;
	case OPC_bs_get_utf16_2x_4I_5x_1L: if (true) {
		int _src = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		int chr = ((EBinMatchState)(reg[_src])).bs_get_utf16((short) (_flags)); if (chr < 0) pc = (_failLabel); else reg[_dest] = (ERT.box(chr));
	} break;
	case OPC_bs_get_utf16_2x_4I_5y_1L: if (true) {
		int _src = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		int chr = ((EBinMatchState)(reg[_src])).bs_get_utf16((short) (_flags)); if (chr < 0) pc = (_failLabel); else stack[sp - (_dest)] = (ERT.box(chr));
	} break;
	case OPC_bs_get_utf16_2y_4I_5x_1L: if (true) {
		int _src = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		int chr = ((EBinMatchState)(stack[sp - (_src)])).bs_get_utf16((short) (_flags)); if (chr < 0) pc = (_failLabel); else reg[_dest] = (ERT.box(chr));
	} break;
	case OPC_bs_get_utf16_2y_4I_5y_1L: if (true) {
		int _src = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		int chr = ((EBinMatchState)(stack[sp - (_src)])).bs_get_utf16((short) (_flags)); if (chr < 0) pc = (_failLabel); else stack[sp - (_dest)] = (ERT.box(chr));
	} break;
	case OPC_bs_get_utf32_2x_4I_5x_1L: if (true) {
		int _src = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		int chr = ((EBinMatchState)(reg[_src])).bs_get_utf32((short) (_flags)); if (chr < 0) pc = (_failLabel); else reg[_dest] = (ERT.box(chr));
	} break;
	case OPC_bs_get_utf32_2x_4I_5y_1L: if (true) {
		int _src = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		int chr = ((EBinMatchState)(reg[_src])).bs_get_utf32((short) (_flags)); if (chr < 0) pc = (_failLabel); else stack[sp - (_dest)] = (ERT.box(chr));
	} break;
	case OPC_bs_get_utf32_2y_4I_5x_1L: if (true) {
		int _src = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		int chr = ((EBinMatchState)(stack[sp - (_src)])).bs_get_utf32((short) (_flags)); if (chr < 0) pc = (_failLabel); else reg[_dest] = (ERT.box(chr));
	} break;
	case OPC_bs_get_utf32_2y_4I_5y_1L: if (true) {
		int _src = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		int chr = ((EBinMatchState)(stack[sp - (_src)])).bs_get_utf32((short) (_flags)); if (chr < 0) pc = (_failLabel); else stack[sp - (_dest)] = (ERT.box(chr));
	} break;
	case OPC_bs_match_string_2x_3c_1L: if (true) {
		int _src = code[pc++];
		int _string = code[pc++];
		int _failLabel = code[pc++];
		if (((EBinMatchState)(reg[_src])).bs_match_string((EBitString)consts[_string]) == null) pc = (_failLabel);
	} break;
	case OPC_bs_match_string_2y_3c_1L: if (true) {
		int _src = code[pc++];
		int _string = code[pc++];
		int _failLabel = code[pc++];
		if (((EBinMatchState)(stack[sp - (_src)])).bs_match_string((EBitString)consts[_string]) == null) pc = (_failLabel);
	} break;
	case OPC_bs_get_integer2_2x_5I_6I_7x_1L: if (true) {
		int _src = code[pc++];
		int _unit = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = ((EBinMatchState)(reg[_src])).bs_get_integer2(((ESmall)prefetched1).intValue(), (short) (_unit), (short) (_flags)); if (tmp == null) pc = (_failLabel); else reg[_dest] = (tmp);
	} break;
	case OPC_bs_get_integer2_2x_5I_6I_7y_1L: if (true) {
		int _src = code[pc++];
		int _unit = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = ((EBinMatchState)(reg[_src])).bs_get_integer2(((ESmall)prefetched1).intValue(), (short) (_unit), (short) (_flags)); if (tmp == null) pc = (_failLabel); else stack[sp - (_dest)] = (tmp);
	} break;
	case OPC_bs_get_integer2_2y_5I_6I_7x_1L: if (true) {
		int _src = code[pc++];
		int _unit = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = ((EBinMatchState)(stack[sp - (_src)])).bs_get_integer2(((ESmall)prefetched1).intValue(), (short) (_unit), (short) (_flags)); if (tmp == null) pc = (_failLabel); else reg[_dest] = (tmp);
	} break;
	case OPC_bs_get_integer2_2y_5I_6I_7y_1L: if (true) {
		int _src = code[pc++];
		int _unit = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = ((EBinMatchState)(stack[sp - (_src)])).bs_get_integer2(((ESmall)prefetched1).intValue(), (short) (_unit), (short) (_flags)); if (tmp == null) pc = (_failLabel); else stack[sp - (_dest)] = (tmp);
	} break;
	case OPC_bs_get_float2_2x_5I_6I_7x_1L: if (true) {
		int _src = code[pc++];
		int _unit = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = ((EBinMatchState)(reg[_src])).bs_get_float2(((ESmall)prefetched1).intValue(), (short) (_unit), (short) (_flags)); if (tmp == null) pc = (_failLabel); else reg[_dest] = (tmp);
	} break;
	case OPC_bs_get_float2_2x_5I_6I_7y_1L: if (true) {
		int _src = code[pc++];
		int _unit = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = ((EBinMatchState)(reg[_src])).bs_get_float2(((ESmall)prefetched1).intValue(), (short) (_unit), (short) (_flags)); if (tmp == null) pc = (_failLabel); else stack[sp - (_dest)] = (tmp);
	} break;
	case OPC_bs_get_float2_2y_5I_6I_7x_1L: if (true) {
		int _src = code[pc++];
		int _unit = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = ((EBinMatchState)(stack[sp - (_src)])).bs_get_float2(((ESmall)prefetched1).intValue(), (short) (_unit), (short) (_flags)); if (tmp == null) pc = (_failLabel); else reg[_dest] = (tmp);
	} break;
	case OPC_bs_get_float2_2y_5I_6I_7y_1L: if (true) {
		int _src = code[pc++];
		int _unit = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = ((EBinMatchState)(stack[sp - (_src)])).bs_get_float2(((ESmall)prefetched1).intValue(), (short) (_unit), (short) (_flags)); if (tmp == null) pc = (_failLabel); else stack[sp - (_dest)] = (tmp);
	} break;
	case OPC_bs_get_binary2_2x_6I_7x_1L: if (true) {
		int _ms = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = ((EBinMatchState)(reg[_ms])).bs_get_binary2(prefetched1, (short) (_flags)); if (tmp==null) pc = (_failLabel); else reg[_dest] = (tmp);
	} break;
	case OPC_bs_get_binary2_2x_6I_7y_1L: if (true) {
		int _ms = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = ((EBinMatchState)(reg[_ms])).bs_get_binary2(prefetched1, (short) (_flags)); if (tmp==null) pc = (_failLabel); else stack[sp - (_dest)] = (tmp);
	} break;
	case OPC_bs_get_binary2_2y_6I_7x_1L: if (true) {
		int _ms = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = ((EBinMatchState)(stack[sp - (_ms)])).bs_get_binary2(prefetched1, (short) (_flags)); if (tmp==null) pc = (_failLabel); else reg[_dest] = (tmp);
	} break;
	case OPC_bs_get_binary2_2y_6I_7y_1L: if (true) {
		int _ms = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = ((EBinMatchState)(stack[sp - (_ms)])).bs_get_binary2(prefetched1, (short) (_flags)); if (tmp==null) pc = (_failLabel); else stack[sp - (_dest)] = (tmp);
	} break;
	case OPC_bs_test_tail2_2x_3I_1L: if (true) {
		int _ms = code[pc++];
		int _bits_left = code[pc++];
		int _failLabel = code[pc++];
		if (! ((EBinMatchState)(reg[_ms])).bs_test_tail2((short) (_bits_left))) pc = (_failLabel);
	} break;
	case OPC_bs_test_tail2_2y_3I_1L: if (true) {
		int _ms = code[pc++];
		int _bits_left = code[pc++];
		int _failLabel = code[pc++];
		if (! ((EBinMatchState)(stack[sp - (_ms)])).bs_test_tail2((short) (_bits_left))) pc = (_failLabel);
	} break;
	case OPC_bs_test_unit_2x_3I_1L: if (true) {
		int _ms = code[pc++];
		int _unit = code[pc++];
		int _failLabel = code[pc++];
		if (! ((EBinMatchState)(reg[_ms])).bs_test_unit((short) (_unit))) pc = (_failLabel);
	} break;
	case OPC_bs_test_unit_2y_3I_1L: if (true) {
		int _ms = code[pc++];
		int _unit = code[pc++];
		int _failLabel = code[pc++];
		if (! ((EBinMatchState)(stack[sp - (_ms)])).bs_test_unit((short) (_unit))) pc = (_failLabel);
	} break;
	case OPC_bs_skip_utf8_2x_4I_1L: if (true) {
		int _ms = code[pc++];
		int _flags = code[pc++];
		int _failLabel = code[pc++];
		if (! ((EBinMatchState)(reg[_ms])).bs_skip_utf8((short) (_flags))) pc = (_failLabel);
	} break;
	case OPC_bs_skip_utf8_2y_4I_1L: if (true) {
		int _ms = code[pc++];
		int _flags = code[pc++];
		int _failLabel = code[pc++];
		if (! ((EBinMatchState)(stack[sp - (_ms)])).bs_skip_utf8((short) (_flags))) pc = (_failLabel);
	} break;
	case OPC_bs_skip_utf16_2x_4I_1L: if (true) {
		int _ms = code[pc++];
		int _flags = code[pc++];
		int _failLabel = code[pc++];
		if (! ((EBinMatchState)(reg[_ms])).bs_skip_utf16((short) (_flags))) pc = (_failLabel);
	} break;
	case OPC_bs_skip_utf16_2y_4I_1L: if (true) {
		int _ms = code[pc++];
		int _flags = code[pc++];
		int _failLabel = code[pc++];
		if (! ((EBinMatchState)(stack[sp - (_ms)])).bs_skip_utf16((short) (_flags))) pc = (_failLabel);
	} break;
	case OPC_bs_skip_utf32_2x_4I_1L: if (true) {
		int _ms = code[pc++];
		int _flags = code[pc++];
		int _failLabel = code[pc++];
		if (! ((EBinMatchState)(reg[_ms])).bs_skip_utf32((short) (_flags))) pc = (_failLabel);
	} break;
	case OPC_bs_skip_utf32_2y_4I_1L: if (true) {
		int _ms = code[pc++];
		int _flags = code[pc++];
		int _failLabel = code[pc++];
		if (! ((EBinMatchState)(stack[sp - (_ms)])).bs_skip_utf32((short) (_flags))) pc = (_failLabel);
	} break;
	case OPC_bs_skip_bits2_2x_4I_5I_1L: if (true) {
		int _ms = code[pc++];
		int _unit = code[pc++];
		int _flags = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = ((EBinMatchState)(reg[_ms])).bs_skip_bits2((prefetched1), (short) (_unit), (short) (_flags)); if (tmp==null) pc = (_failLabel);
	} break;
	case OPC_bs_skip_bits2_2y_4I_5I_1L: if (true) {
		int _ms = code[pc++];
		int _unit = code[pc++];
		int _flags = code[pc++];
		int _failLabel = code[pc++];
		EObject tmp = ((EBinMatchState)(stack[sp - (_ms)])).bs_skip_bits2((prefetched1), (short) (_unit), (short) (_flags)); if (tmp==null) pc = (_failLabel);
	} break;
	case OPC_bs_utf8_size_3x: if (true) {
		int _dest = code[pc++];
		reg[_dest] = (EBitStringBuilder.bs_utf8_size(prefetched1)); // Label unused??
	} break;
	case OPC_bs_utf8_size_3y: if (true) {
		int _dest = code[pc++];
		stack[sp - (_dest)] = (EBitStringBuilder.bs_utf8_size(prefetched1)); // Label unused??
	} break;
	case OPC_bs_utf16_size_3x: if (true) {
		int _dest = code[pc++];
		reg[_dest] = (EBitStringBuilder.bs_utf16_size(prefetched1)); // Label unused??
	} break;
	case OPC_bs_utf16_size_3y: if (true) {
		int _dest = code[pc++];
		stack[sp - (_dest)] = (EBitStringBuilder.bs_utf16_size(prefetched1)); // Label unused??
	} break;
	case OPC_bs_save2_1x_2I: if (true) {
		int _ms = code[pc++];
		int _pos = code[pc++];
		EObject ms = reg[_ms]; int pos = (short) (_pos); if (pos==-1) EBinMatchState.bs_save2_start(ms); else EBinMatchState.bs_save2(ms, pos);
	} break;
	case OPC_bs_save2_1y_2I: if (true) {
		int _ms = code[pc++];
		int _pos = code[pc++];
		EObject ms = stack[sp - (_ms)]; int pos = (short) (_pos); if (pos==-1) EBinMatchState.bs_save2_start(ms); else EBinMatchState.bs_save2(ms, pos);
	} break;
	case OPC_bs_restore2_1x_2I: if (true) {
		int _ms = code[pc++];
		int _pos = code[pc++];
		EObject ms = reg[_ms]; int pos = (short) (_pos); if (pos==-1) EBinMatchState.bs_restore2_start(ms); else EBinMatchState.bs_restore2(ms, pos);
	} break;
	case OPC_bs_restore2_1y_2I: if (true) {
		int _ms = code[pc++];
		int _pos = code[pc++];
		EObject ms = stack[sp - (_ms)]; int pos = (short) (_pos); if (pos==-1) EBinMatchState.bs_restore2_start(ms); else EBinMatchState.bs_restore2(ms, pos);
	} break;
	case OPC_bs_init_writable: if (true) {
		bit_string_builder = EBitStringBuilder.bs_init_writable(reg[0]); reg[0] = bit_string_builder.bitstring();
	} break;
	case OPC_bs_init2_5I_6x: if (true) {
		int _flags = code[pc++];
		int _dest = code[pc++];
		bit_string_builder = ERT.bs_init(((ESmall)prefetched1).intValue(), (short) (_flags)); reg[_dest] = (bit_string_builder.bitstring());
	} break;
	case OPC_bs_init2_5I_6y: if (true) {
		int _flags = code[pc++];
		int _dest = code[pc++];
		bit_string_builder = ERT.bs_init(((ESmall)prefetched1).intValue(), (short) (_flags)); stack[sp - (_dest)] = (bit_string_builder.bitstring());
	} break;
	case OPC_bs_init_bits_5I_6x: if (true) {
		int _flags = code[pc++];
		int _dest = code[pc++];
		bit_string_builder = ERT.bs_initBits(((ESmall)prefetched1).intValue(), (short) (_flags)); reg[_dest] = (bit_string_builder.bitstring());
	} break;
	case OPC_bs_init_bits_5I_6y: if (true) {
		int _flags = code[pc++];
		int _dest = code[pc++];
		bit_string_builder = ERT.bs_initBits(((ESmall)prefetched1).intValue(), (short) (_flags)); stack[sp - (_dest)] = (bit_string_builder.bitstring());
	} break;
	case OPC_bs_put_string_1c: if (true) {
		int _value = code[pc++];
		bit_string_builder.put_string((EString)consts[_value]);
	} break;
	case OPC_bs_put_integer_3I_4I: if (true) {
		int _unit = code[pc++];
		int _flags = code[pc++];
		bit_string_builder.put_integer(prefetched1, (short) (_unit) * ((ESmall)prefetched2).intValue(), (short) (_flags));
	} break;
	case OPC_bs_put_binary_3I_4I: if (true) {
		int _unit = code[pc++];
		int _flags = code[pc++];
		EInteger size = prefetched1.testInteger(); int actualSize = (size==null)? -1 : size.intValue() * (short) (_unit); bit_string_builder.put_bitstring(prefetched2, actualSize, (short) (_flags));
	} break;
	case OPC_bs_put_float_3I_4I: if (true) {
		int _unit = code[pc++];
		int _flags = code[pc++];
		bit_string_builder.put_float(prefetched1, (short) (_unit) * ((ESmall)prefetched2).intValue(), (short) (_flags));
	} break;
	case OPC_bs_put_utf8_2I: if (true) {
		int _flags = code[pc++];
		bit_string_builder.put_utf8(prefetched1, (short) (_flags));
	} break;
	case OPC_bs_put_utf16_2I: if (true) {
		int _flags = code[pc++];
		bit_string_builder.put_utf16(prefetched1, (short) (_flags));
	} break;
	case OPC_bs_put_utf32_2I: if (true) {
		int _flags = code[pc++];
		bit_string_builder.put_utf32(prefetched1, (short) (_flags));
	} break;
	case OPC_bs_add_4I_5x_1L: if (true) {
		int _yunit = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		try {int xval = ERT.unboxToInt(prefetched1), yval = ERT.unboxToInt(prefetched2); reg[_dest] = (ERT.box(xval + yval * (short) (_yunit)));} catch (Exception e) {pc = (_onFail);}
	} break;
	case OPC_bs_add_4I_5x_0nolabel: if (true) {
		int _yunit = code[pc++];
		int _dest = code[pc++];
		try {int xval = ERT.unboxToInt(prefetched1), yval = ERT.unboxToInt(prefetched2); reg[_dest] = (ERT.box(xval + yval * (short) (_yunit)));} catch (Exception e) {pc = nofailLabel();}
	} break;
	case OPC_bs_add_4I_5y_1L: if (true) {
		int _yunit = code[pc++];
		int _dest = code[pc++];
		int _onFail = code[pc++];
		try {int xval = ERT.unboxToInt(prefetched1), yval = ERT.unboxToInt(prefetched2); stack[sp - (_dest)] = (ERT.box(xval + yval * (short) (_yunit)));} catch (Exception e) {pc = (_onFail);}
	} break;
	case OPC_bs_add_4I_5y_0nolabel: if (true) {
		int _yunit = code[pc++];
		int _dest = code[pc++];
		try {int xval = ERT.unboxToInt(prefetched1), yval = ERT.unboxToInt(prefetched2); stack[sp - (_dest)] = (ERT.box(xval + yval * (short) (_yunit)));} catch (Exception e) {pc = nofailLabel();}
	} break;
	case OPC_bs_append_5I_7I_8x: if (true) {
		int _unit = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		bit_string_builder = EBitStringBuilder.bs_append(prefetched1, ERT.unboxToInt(prefetched2), (short) (_unit), (short) (_flags)); reg[_dest] = (bit_string_builder.bitstring());
	} break;
	case OPC_bs_append_5I_7I_8y: if (true) {
		int _unit = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		bit_string_builder = EBitStringBuilder.bs_append(prefetched1, ERT.unboxToInt(prefetched2), (short) (_unit), (short) (_flags)); stack[sp - (_dest)] = (bit_string_builder.bitstring());
	} break;
	case OPC_bs_private_append_3I_5I_6x: if (true) {
		int _unit = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		bit_string_builder = EBitStringBuilder.bs_append(prefetched1, ERT.unboxToInt(prefetched2), (short) (_unit), (short) (_flags)); reg[_dest] = (bit_string_builder.bitstring());
	} break;
	case OPC_bs_private_append_3I_5I_6y: if (true) {
		int _unit = code[pc++];
		int _flags = code[pc++];
		int _dest = code[pc++];
		bit_string_builder = EBitStringBuilder.bs_append(prefetched1, ERT.unboxToInt(prefetched2), (short) (_unit), (short) (_flags)); stack[sp - (_dest)] = (bit_string_builder.bitstring());
	} break;
	case OPC_bs_context_to_binary_1x: if (true) {
		int _srcdest = code[pc++];
		reg[_srcdest] = (EBinMatchState.bs_context_to_binary(reg[_srcdest]));
	} break;
	case OPC_bs_context_to_binary_1y: if (true) {
		int _srcdest = code[pc++];
		stack[sp - (_srcdest)] = (EBinMatchState.bs_context_to_binary(stack[sp - (_srcdest)]));
	} break;
	case OPC_K_catch_2L_1y: if (true) {
		int _lbl = code[pc++];
		int _y = code[pc++];
		stack[sp - (_y)] = (exh); exh = ( false ? new TryExceptionHandler((_lbl), exh) : new CatchExceptionHandler((_lbl), exh));
	} break;
	case OPC_K_try_2L_1y: if (true) {
		int _lbl = code[pc++];
		int _y = code[pc++];
		stack[sp - (_y)] = (exh); exh = ( true ? new TryExceptionHandler((_lbl), exh) : new CatchExceptionHandler((_lbl), exh));
	} break;
	case OPC_catch_end_1y: if (true) {
		int _y = code[pc++];
		{exh = (ExceptionHandlerStackElement) stack[sp - (_y)];};
	} break;
	case OPC_try_end_1y: if (true) {
		int _y = code[pc++];
		{exh = (ExceptionHandlerStackElement) stack[sp - (_y)];};
	} break;
	case OPC_try_case_1y: if (true) {
		int _y = code[pc++];
		{exh = (ExceptionHandlerStackElement) stack[sp - (_y)];}; {/* Exception deconstruction done by TryExceptionHandler. */}
	} break;
	case OPC_raise: if (true) {
		reg[0] = ERT.raise(prefetched1, prefetched2);
	} break;
	case OPC_fmove_1c_2x: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		reg[_dst] = (ErlBif.float$n((EObject)consts[_src]));
	} break;
	case OPC_fmove_1c_2y: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		stack[sp - (_dst)] = (ErlBif.float$n((EObject)consts[_src]));
	} break;
	case OPC_fmove_1c_2f: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		freg[_dst] = (ErlBif.float$n((EObject)consts[_src]));
	} break;
	case OPC_fmove_1x_2x: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		reg[_dst] = (ErlBif.float$n((EObject)reg[_src]));
	} break;
	case OPC_fmove_1x_2y: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		stack[sp - (_dst)] = (ErlBif.float$n((EObject)reg[_src]));
	} break;
	case OPC_fmove_1x_2f: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		freg[_dst] = (ErlBif.float$n((EObject)reg[_src]));
	} break;
	case OPC_fmove_1y_2x: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		reg[_dst] = (ErlBif.float$n((EObject)stack[sp - (_src)]));
	} break;
	case OPC_fmove_1y_2y: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		stack[sp - (_dst)] = (ErlBif.float$n((EObject)stack[sp - (_src)]));
	} break;
	case OPC_fmove_1y_2f: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		freg[_dst] = (ErlBif.float$n((EObject)stack[sp - (_src)]));
	} break;
	case OPC_fmove_1f_2x: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		reg[_dst] = (ErlBif.float$n((EObject)freg[_src]));
	} break;
	case OPC_fmove_1f_2y: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		stack[sp - (_dst)] = (ErlBif.float$n((EObject)freg[_src]));
	} break;
	case OPC_fmove_1f_2f: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		freg[_dst] = (ErlBif.float$n((EObject)freg[_src]));
	} break;
	case OPC_fconv_1c_2x: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		reg[_dst] = (ErlBif.float$n((EObject)consts[_src]));
	} break;
	case OPC_fconv_1c_2y: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		stack[sp - (_dst)] = (ErlBif.float$n((EObject)consts[_src]));
	} break;
	case OPC_fconv_1c_2f: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		freg[_dst] = (ErlBif.float$n((EObject)consts[_src]));
	} break;
	case OPC_fconv_1x_2x: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		reg[_dst] = (ErlBif.float$n((EObject)reg[_src]));
	} break;
	case OPC_fconv_1x_2y: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		stack[sp - (_dst)] = (ErlBif.float$n((EObject)reg[_src]));
	} break;
	case OPC_fconv_1x_2f: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		freg[_dst] = (ErlBif.float$n((EObject)reg[_src]));
	} break;
	case OPC_fconv_1y_2x: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		reg[_dst] = (ErlBif.float$n((EObject)stack[sp - (_src)]));
	} break;
	case OPC_fconv_1y_2y: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		stack[sp - (_dst)] = (ErlBif.float$n((EObject)stack[sp - (_src)]));
	} break;
	case OPC_fconv_1y_2f: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		freg[_dst] = (ErlBif.float$n((EObject)stack[sp - (_src)]));
	} break;
	case OPC_fconv_1f_2x: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		reg[_dst] = (ErlBif.float$n((EObject)freg[_src]));
	} break;
	case OPC_fconv_1f_2y: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		stack[sp - (_dst)] = (ErlBif.float$n((EObject)freg[_src]));
	} break;
	case OPC_fconv_1f_2f: if (true) {
		int _src = code[pc++];
		int _dst = code[pc++];
		freg[_dst] = (ErlBif.float$n((EObject)freg[_src]));
	} break;
	case OPC_fadd_2f_3f_4f: if (true) {
		int _a = code[pc++];
		int _b = code[pc++];
		int _dst = code[pc++];
		freg[_dst] = (ERT.box(ErlBif.fadd(freg[_a].value, freg[_b].value)));
	} break;
	case OPC_fsub_2f_3f_4f: if (true) {
		int _a = code[pc++];
		int _b = code[pc++];
		int _dst = code[pc++];
		freg[_dst] = (ERT.box(ErlBif.fsub(freg[_a].value, freg[_b].value)));
	} break;
	case OPC_fmul_2f_3f_4f: if (true) {
		int _a = code[pc++];
		int _b = code[pc++];
		int _dst = code[pc++];
		freg[_dst] = (ERT.box(ErlBif.fmul(freg[_a].value, freg[_b].value)));
	} break;
	case OPC_fdiv_2f_3f_4f: if (true) {
		int _a = code[pc++];
		int _b = code[pc++];
		int _dst = code[pc++];
		freg[_dst] = (ERT.box(ErlBif.fdiv(freg[_a].value, freg[_b].value)));
	} break;
//---------- Prefetch decoding:
case OPC_FETCH_S + (0 * SIZE_FETCH_S + S_VARIANT_c): {
int _prefetched1 = code[pc++];
prefetched1 = consts[_prefetched1];
} break;
case OPC_FETCH_S + (0 * SIZE_FETCH_S + S_VARIANT_x): {
int _prefetched1 = code[pc++];
prefetched1 = reg[_prefetched1];
} break;
case OPC_FETCH_S + (0 * SIZE_FETCH_S + S_VARIANT_y): {
int _prefetched1 = code[pc++];
prefetched1 = stack[sp - (_prefetched1)];
} break;
case OPC_FETCH_S_S + ((0 * SIZE_FETCH_S + S_VARIANT_c) * SIZE_FETCH_S + S_VARIANT_c): {
int _prefetched1 = code[pc++];
int _prefetched2 = code[pc++];
prefetched1 = consts[_prefetched1];
prefetched2 = consts[_prefetched2];
} break;
case OPC_FETCH_S_S + ((0 * SIZE_FETCH_S + S_VARIANT_x) * SIZE_FETCH_S + S_VARIANT_c): {
int _prefetched1 = code[pc++];
int _prefetched2 = code[pc++];
prefetched1 = reg[_prefetched1];
prefetched2 = consts[_prefetched2];
} break;
case OPC_FETCH_S_S + ((0 * SIZE_FETCH_S + S_VARIANT_y) * SIZE_FETCH_S + S_VARIANT_c): {
int _prefetched1 = code[pc++];
int _prefetched2 = code[pc++];
prefetched1 = stack[sp - (_prefetched1)];
prefetched2 = consts[_prefetched2];
} break;
case OPC_FETCH_S_S + ((0 * SIZE_FETCH_S + S_VARIANT_c) * SIZE_FETCH_S + S_VARIANT_x): {
int _prefetched1 = code[pc++];
int _prefetched2 = code[pc++];
prefetched1 = consts[_prefetched1];
prefetched2 = reg[_prefetched2];
} break;
case OPC_FETCH_S_S + ((0 * SIZE_FETCH_S + S_VARIANT_x) * SIZE_FETCH_S + S_VARIANT_x): {
int _prefetched1 = code[pc++];
int _prefetched2 = code[pc++];
prefetched1 = reg[_prefetched1];
prefetched2 = reg[_prefetched2];
} break;
case OPC_FETCH_S_S + ((0 * SIZE_FETCH_S + S_VARIANT_y) * SIZE_FETCH_S + S_VARIANT_x): {
int _prefetched1 = code[pc++];
int _prefetched2 = code[pc++];
prefetched1 = stack[sp - (_prefetched1)];
prefetched2 = reg[_prefetched2];
} break;
case OPC_FETCH_S_S + ((0 * SIZE_FETCH_S + S_VARIANT_c) * SIZE_FETCH_S + S_VARIANT_y): {
int _prefetched1 = code[pc++];
int _prefetched2 = code[pc++];
prefetched1 = consts[_prefetched1];
prefetched2 = stack[sp - (_prefetched2)];
} break;
case OPC_FETCH_S_S + ((0 * SIZE_FETCH_S + S_VARIANT_x) * SIZE_FETCH_S + S_VARIANT_y): {
int _prefetched1 = code[pc++];
int _prefetched2 = code[pc++];
prefetched1 = reg[_prefetched1];
prefetched2 = stack[sp - (_prefetched2)];
} break;
case OPC_FETCH_S_S + ((0 * SIZE_FETCH_S + S_VARIANT_y) * SIZE_FETCH_S + S_VARIANT_y): {
int _prefetched1 = code[pc++];
int _prefetched2 = code[pc++];
prefetched1 = stack[sp - (_prefetched1)];
prefetched2 = stack[sp - (_prefetched2)];
} break;

							default:
								throw new Error("Unimplemented internal opcode: "+opcode+" at "+module_name()+"@"+(pc-1));
							}
						}
					} catch (ErlangException e) {
						if (stack==null) { // POST_CALL():
							stack=proc.stack; assert(proc.sp==sp);
						}

						if (exh != null) {
							proc.last_exception = e;
							exh.catchAction(e,reg);
							pc = exh.pc;
							continue;
						} else throw(e);
					} catch (RuntimeException e) {
						if (ERT.DEBUG) {
							System.err.println("DB| Error when interpreting "+module_name()+"@"+(pc-1)+"; opcode="+code[last_pc]+"; prefetched1="+prefetched1+"; prefetched2="+prefetched2+" : "+e);
							System.err.println("Code:");
							for (int i=Math.max(0,pc-15); i<Math.min(code.length,pc+5); i++) System.err.println("  "+i+": "+(int)code[i]);
						}
						throw e;
					}
			}
		} // class Function

		class Closure extends Function {
			final EObject[] env;

			public Closure(EObject[] env, int start_pc) {
				super(start_pc);
				this.env = env;
			}

			public EObject invoke(final EProc proc, final EObject[] args) throws Pausable {
// 				System.err.println("INT| Closure invoked @ "+start_pc);
				int argCnt = args.length;
				int envCnt = env.length;
				EObject[] reg = new EObject[1024]; //??
				for (int i=0; i<argCnt; i++) {reg[i] = args[i];} //??
				for (int i=0; i<envCnt; i++) {reg[argCnt+i] = env[i];}
				//for (int i=0; i<argCnt+envCnt; i++) System.err.println("INT| reg#"+i+"="+reg[i]);
				return interpret(proc, start_pc, reg);
			}
		}

    } // class Module
} // class Interpreter
