#include "simulator.h"

void Simulator::reset(uint64_t cycles) {
    c->reset = 1;
    step(cycles);
    c->reset = 0;
}

void Simulator::step(uint64_t cycles) {
    for (uint64_t i = 0; i < cycles; i ++) {
        printf("Step %llu --\n", i);
        c->clock = 1;
        c->eval();
        c->clock = 0;
        c->eval();
        check_bus();

        if (halt) {
            return;
        }
    }
}

void Simulator::check_bus() {
    c->io_iRead_data_valid = 0;
    c->io_dRead_data_valid = 0;

    if (c->io_iRead_addressInfo_valid == 1) {
        printf("Reading %016lx <- %016llx\n", c->io_iRead_addressInfo_bits_address,
               memory.read(c->io_iRead_addressInfo_bits_address, sz_double));
        c->io_iRead_data_valid = 1;
        c->io_iRead_data_bits = memory.read(c->io_iRead_addressInfo_bits_address, sz_double);
    }

    if (c->io_dRead_addressInfo_valid == 1) {
        c->io_dRead_data_valid = 1;
        c->io_dRead_data_bits = memory.read(c->io_dRead_addressInfo_bits_address,
                                            c->io_dRead_addressInfo_bits_size);
    }

    if (c->io_dWrite_storeInfo_valid == 1) {
        printf("Write: %lx %d %lx\n", c->io_dWrite_storeInfo_bits_addressInfo_address,
               c->io_dRead_addressInfo_bits_size,
               c->io_dWrite_storeInfo_bits_data);
        if ((c->io_dWrite_storeInfo_bits_addressInfo_address & 0xFFFFFFFFLL) == 0xFFFF0000LL) {
            printf("%c", (char)c->io_dWrite_storeInfo_bits_data);
        } else if ((c->io_dWrite_storeInfo_bits_addressInfo_address & 0xFFFFFFFFLL) == 0xFFFF0010LL) {
            if (c->io_dWrite_storeInfo_bits_data != 0) {
                printf("System halt with error %lu.\n", c->io_dWrite_storeInfo_bits_data);
            } else {
                puts("System halt.");
            }

            halt = true;
        } else {
            memory.write(c->io_dWrite_storeInfo_bits_addressInfo_address,
                         c->io_dRead_addressInfo_bits_size,
                         c->io_dWrite_storeInfo_bits_data);
        }
    }
}

void Simulator::bus_init(const char *elf_path) {
    c->io_iRead_addressInfo_ready = 1;
    c->io_dRead_addressInfo_ready = 1;
    c->io_dWrite_storeInfo_ready = 1;

    memory.init(elf_path);
}
