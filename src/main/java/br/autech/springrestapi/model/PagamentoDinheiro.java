package br.autech.springrestapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity(name = "PAGAMENTO_DINHEIRO")
@JsonTypeName("dinheiro")
public class PagamentoDinheiro extends Pagamento{
}
