package br.autech.springrestapi.controller;

import br.autech.springrestapi.dtos.ClienteNovoDTO;
import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.service.ClienteService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/cliente")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<Cliente>> findAll(){
        List<Cliente> clienteList = clienteService.findAll();
        return ResponseEntity.ok().body(clienteList);
    }

    @RequestMapping(value = "/{cnpjCpf}", method = RequestMethod.GET)
    public ResponseEntity<Cliente> findByCnpjCpf(@PathVariable String cnpjCpf){
        return ResponseEntity.ok().body(clienteService.findByCnpjCpf(cnpjCpf));
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ResponseEntity<Page<Cliente>> search(
            @RequestParam(value = "searchId", required = false, defaultValue = "1") int searchId,
            @RequestParam(value = "searchTerm", required = false, defaultValue = "")  String searchTerm,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size
    ){
        Page<Cliente> pagenation = clienteService.search(searchId, searchTerm, page, size);
        return ResponseEntity.ok().body(pagenation);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Void> insert(@Valid @RequestBody ClienteNovoDTO clienteNovoDTO){
        Cliente clienteEntity = new Cliente();
        BeanUtils.copyProperties(clienteNovoDTO, clienteEntity);
        clienteEntity = clienteService.insert(clienteEntity);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(clienteEntity.getCnpjCpf()).toUri();
        return ResponseEntity.created(uri).build();
    }

    @RequestMapping(value = "/{cnpjCpf}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> delete(@PathVariable String cnpjCpf){
        clienteService.delete(cnpjCpf);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.PUT)
    public ResponseEntity<Cliente> update(@RequestBody Cliente cliente){

        return ResponseEntity.ok().body(this.clienteService.update(cliente));
    }

    @RequestMapping(value = "/chave/{cnpjCpf}",method = RequestMethod.GET)
    public ResponseEntity<String> getChave(@PathVariable String cnpjCpf){
        return ResponseEntity.ok().body(clienteService.getChave(cnpjCpf).toString());
    }

    @RequestMapping(value = "/id/{id}", method = RequestMethod.GET)
    public ResponseEntity<Cliente> findById(@PathVariable String id){
        return ResponseEntity.ok().body(clienteService.findById(id));
    }

    @GetMapping(value = "/count/ativo/{ativo}")
    public ResponseEntity<Long> countClientesByAtivo(@PathVariable String ativo){
        return ResponseEntity.ok().body(clienteService.countClientesByAtivo(ativo));
    }

    @GetMapping(value = "/count/bloqueado/{bloqueado}")
    public ResponseEntity<Long> countClientesByBloqueado(@PathVariable String bloqueado){
        return ResponseEntity.ok().body(clienteService.countClientesByBloqueado(bloqueado));
    }

}
