/*
 *
 *  Managed Data Structures
 *  Copyright © 2016 Hewlett Packard Enterprise Development Company LP.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  As an exception, the copyright holders of this Library grant you permission
 *  to (i) compile an Application with the Library, and (ii) distribute the 
 *  Application containing code generated by the Library and added to the 
 *  Application during this compilation process under terms of your choice, 
 *  provided you also meet the terms and conditions of the Application license.
 *
 */

/*
 * core_record.h
 *
 *  Created on: Oct 21, 2014
 *      Author: evank
 */

#ifndef CORE_RECORD_H_
#define CORE_RECORD_H_

#include "core/core_fwd.h"
#include "core/core_type.h"
#include "core/core_msv.h"
#include "core/core_globals.h"
#include "core/core_strings.h"
#include "ruts/uniform_key.h"
#include "ruts/cas_loop.h"
#include "mpgc/gc_vector.h"
#include <iostream>

namespace mds {
  namespace core {

    struct record_field_base : public exportable, with_uniform_id
    {
      const kind type_kind;
      const gc_ptr<interned_string> name;
      const std::size_t num;
      mutable validity validity_state;
      const gc_ptr<record_type> r_type;
      const gc_ptr<const managed_type_base> f_type_base;

      record_field_base(
          gc_token &gc,
          kind k,
          const gc_ptr<interned_string> &n,
          std::size_t i,
          const gc_ptr<record_type> &rt,
          const gc_ptr<const managed_type_base> &ftb
          )
    : exportable{gc},
      type_kind{k},
      name{n},
      num{i},
      validity_state{validity::unchecked},
      r_type{rt},
      f_type_base{ftb}
      {}

      static const auto &descriptor() {
        static gc_descriptor d =
	  GC_DESC(record_field_base)
	  .WITH_SUPER(with_uniform_id)
	  .WITH_FIELD(&record_field_base::type_kind)
	  .WITH_FIELD(&record_field_base::name)
	  .WITH_FIELD(&record_field_base::num)
	  .WITH_FIELD(&record_field_base::validity_state)
	  .WITH_FIELD(&record_field_base::r_type)
	  .WITH_FIELD(&record_field_base::f_type_base);
        return d;
      }

      bool compatible_with(const gc_ptr<record_field_base> &other) const ;
      bool is_valid() const;


    };

    struct managed_record : public managed_composite, public with_uniform_id
    {
      const gc_ptr<const record_type> r_type;
      using atomic_msv = std::atomic<gc_ptr<msv_base>>;
      mutable gc_array_ptr<atomic_msv> fields;
      explicit managed_record(gc_token &, const gc_ptr<const record_type> &rt);

      static const auto &descriptor() {
        static gc_descriptor d =
	  GC_DESC(managed_record)
	  .WITH_SUPER(managed_composite)
	  .WITH_SUPER(with_uniform_id)
	  .WITH_FIELD(&managed_record::r_type)
	  .WITH_FIELD(&managed_record::fields);
        return d;
      }

      template <kind K>
      gc_ptr<msv<K>> field(std::size_t i,
                           const gc_ptr<const record_field<K>> &field,
                           bool create_if_null = false) const;
    };

    template <kind K>
    struct record_field : record_field_base
    {
      const gc_ptr<const kind_type<K>> f_type;

      record_field(gc_token &gc,
                   const gc_ptr<interned_string> &name,
                   std::size_t num,
                   const gc_ptr<record_type> &rt,
                   const gc_ptr<const kind_type<K>> &ft)
    :
      record_field_base{gc, K, name, num, rt, ft},
      f_type{ft}
      {}

      static const auto &descriptor() {
        static gc_descriptor d =
	  GC_DESC(record_field)
	  .template WITH_SUPER(record_field_base)
	  .template WITH_FIELD(&record_field::f_type);
        return d;
      }

      kind_mv<K> read(const gc_ptr<const managed_record> &r,
                      const gc_ptr<branch> &b,
                      const gc_ptr<iso_context> &ctxt) const;
      kind_mv<K> read_frozen(const gc_ptr<const managed_record> &r,
                             const gc_ptr<branch> &b,
                             const gc_ptr<iso_context> &ctxt) const;
      bool has_value(const gc_ptr<const managed_record> &r,
                     const gc_ptr<branch> &b,
                     const gc_ptr<iso_context> &ctxt) const;
      kind_mv<K> modify(const gc_ptr<const managed_record> &r,
                  const gc_ptr<branch> &b,
                  const gc_ptr<iso_context> &ctxt,
                  modify_op op, const kind_mv<K> &arg, res_mode resolving = res_mode::non_resolving) const;
      kind_mv<K> write(const gc_ptr<const managed_record> &r,
                 const gc_ptr<branch> &b,
                 const gc_ptr<iso_context> &ctxt,
                 const kind_mv<K> &val, res_mode resolving = res_mode::non_resolving) const {
        return modify(r, b, ctxt, modify_op::set, val, resolving);
      }
      template <typename T = kind_mv<K>, typename = std::enable_if_t<std::is_arithmetic<T>::value>>
	kind_mv<K> add(const gc_ptr<const managed_record> &r,
                   const gc_ptr<branch> &b,
                   const gc_ptr<iso_context> &ctxt,
                   const kind_mv<K> &delta, res_mode resolving = res_mode::non_resolving) const
      {
        return modify(r, b, ctxt, modify_op::add, delta, resolving);
      }
      template <typename T = kind_mv<K>, typename = std::enable_if_t<std::is_arithmetic<T>::value>>
	kind_mv<K> sub(const gc_ptr<const managed_record> &r,
                   const gc_ptr<branch> &b,
                   const gc_ptr<iso_context> &ctxt,
                   const kind_mv<K> &delta, res_mode resolving = res_mode::non_resolving) const
      {
        return modify(r, b, ctxt, modify_op::sub, delta, resolving);
      }
      template <typename T = kind_mv<K>, typename = std::enable_if_t<std::is_arithmetic<T>::value>>
	kind_mv<K> mul(const gc_ptr<const managed_record> &r,
                   const gc_ptr<branch> &b,
                   const gc_ptr<iso_context> &ctxt,
                   const kind_mv<K> &delta, res_mode resolving = res_mode::non_resolving) const
      {
        return modify(r, b, ctxt, modify_op::mul, delta, resolving);
      }
      template <typename T = kind_mv<K>, typename = std::enable_if_t<std::is_arithmetic<T>::value>>
	kind_mv<K> div(const gc_ptr<const managed_record> &r,
                   const gc_ptr<branch> &b,
                   const gc_ptr<iso_context> &ctxt,
                   const kind_mv<K> &delta, res_mode resolving = res_mode::non_resolving) const
      {
        return modify(r, b, ctxt, modify_op::div, delta, resolving);
      }

      kind_mv<K> set_to_parent(const gc_ptr<const managed_record> &r,
                         const gc_ptr<branch> &b,
                         const gc_ptr<iso_context> &ctxt,
                         res_mode resolving = res_mode::non_resolving) const
      {
        return modify(r, b, ctxt, modify_op::parent_val, kind_mv<K>{}, resolving);
      }
      kind_mv<K> resolve_to_parent(const gc_ptr<const managed_record> &r,
                             const gc_ptr<branch> &b,
                             const gc_ptr<iso_context> &ctxt) const
      {
        return set_to_parent(r, b, ctxt, res_mode::resolving);
      }
      kind_mv<K> resolve_to_current(const gc_ptr<const managed_record> &r,
                              const gc_ptr<branch> &b,
                              const gc_ptr<iso_context> &ctxt) const
      {
        return modify(r, b, ctxt, modify_op::current_val, kind_mv<K>{}, res_mode::resolving);
      }
      kind_mv<K> roll_back(const gc_ptr<const managed_record> &r,
                     const gc_ptr<branch> &b,
                     const gc_ptr<iso_context> &ctxt,
                     res_mode resolving = res_mode::non_resolving) const
      {
        return modify(r, b, ctxt, modify_op::last_stable_val, kind_mv<K>{}, resolving);
      }
      kind_mv<K> resolve_by_rollback(const gc_ptr<const managed_record> &r,
                               const gc_ptr<branch> &b,
                               const gc_ptr<iso_context> &ctxt) const
      {
        return roll_back(r, b, ctxt, res_mode::resolving);
      }
    };



    class record_type : public managed_type<kind::RECORD> {
      const gc_ptr<const record_type> _super;
      const gc_ptr<interned_string> _name;
      gc_vector<record_field_base> _fields;
      /*
       * We assume that the creation takes place in a single-threaded
       * or synchronized manner, so we don't worry about race conditions
       * on _created or _fields.  Once it's created, before it's put in the table,
       * nobody else will be allowed to add any more fields.
       */
      mutable bool _created = false;
      mutable bool _valid = true;
      mutable gc_ptr<const record_type> _forward = nullptr;

      class private_ctor {};

      gc_ptr<const record_type> try_to_create() const;
    public:
      record_type(gc_token &gc,
                  private_ctor,
                  const gc_ptr<interned_string> &name,
                  const gc_ptr<const record_type> &super = nullptr)
      : managed_type<kind::RECORD>{gc},
        _super{super},
        _name{name}
      {
        // std::cout << "Declaring " << name << std::endl;
        if (_super != nullptr) {
          // std::cout << "  Derives from " << _super->name() << std::endl;
          _fields = _super->fields();
          // std::cout << "  Initial fields: " << std::endl;
          // for (const auto &f : _fields) {
          //   std:: cout << "    " << f->num << ": " << f->name << std::endl;
          // }
        }
      }

      static const auto &descriptor() {
        static gc_descriptor d =
	  GC_DESC(record_type)
	  .WITH_SUPER(managed_type<kind::RECORD>)
	  .WITH_FIELD(&record_type::_super)
	  .WITH_FIELD(&record_type::_name)
	  .WITH_FIELD(&record_type::_fields)
	  .WITH_FIELD(&record_type::_created)
	  .WITH_FIELD(&record_type::_valid)
	  .WITH_FIELD(&record_type::_forward);
        return d;
      }
      /*
       * Returns nullptr if this is the one in the table, or the
       * equivalent one (this one's _forward ptr).  If incompatible with
       * the one in the table, throws incompatible_record_type_ex.
       */
      gc_ptr<const record_type> ensure_created() const {
        // If we're already created, there's nothing to do.
        if (_created) {
          // Either null if no forward or who we forward to.
          return _forward;
        }
        // If we've already proven that we're incompatible, just say
        // so again
        if (!_valid) {
          throw incompatible_record_type_ex{};
        }
        return try_to_create();
      }
      bool compare_types(const gc_ptr<const record_type> &other) const {
        /*
         * record types are only the same if identical or if
         * one forwards to the other.
         */
        return this == other
            || (other != nullptr &&
                  (other == _forward ||
                   this == other->_forward ||
                   _forward == other->_forward));
      }
      bool is_super_of(gc_ptr<const record_type> other) const {
       using std::string;
       //       auto my_name = name();
       //       auto their_name = other->name();
       // std::cout << "Is " << string(my_name->cbegin(), my_name->cend())
       //     << " a super of "
       //     << string(their_name->cbegin(), their_name->cend())
       //     << "?" << std::endl;

        for (; other != nullptr; other = other->_super) {
	  // their_name = other->name();
	  // std::cout << "Checking " << string(my_name->cbegin(), my_name->cend())
	  //     << " against "
	  //     << string(their_name->cbegin(), their_name->cend())
	  //     << std::endl;
          if (compare_types(other)) {
            return true;
          }
        }
        return false;
      }
      bool is_created() const {
        return _created;
      }

      gc_ptr<interned_string> name() const {
        return _name;
      }

      gc_ptr<const record_type> super_type() const {
        return _super;
      }

      std::size_t n_fields() const {
        return _forward != nullptr ? _forward->n_fields() : _fields.size();
      }

      const gc_vector<record_field_base> &fields() const {
        return _forward != nullptr ? _forward->fields() : _fields;
      }
      
      static gc_ptr<const record_type> find(const gc_ptr<interned_string> &name) {
        auto rt = record_type_table->get(name);
        // if (rt != nullptr) {
        //   std::cout << "Found " << rt->name() << std::endl;
        //   if (rt->_super != nullptr) {
        //     std::cout << "  Derives from " << rt->_super->name() << std::endl;
        //   }
        //   if (rt->n_fields() > 0) {
        //     std::cout << "  Fields: " << std::endl;
        //     for (const auto &f : rt->fields()) {
        //       std:: cout << "    " << f->num << ": " << f->name << std::endl;
        //     }
        //   }
        // }
        return rt;
      }
      static gc_ptr<record_type> declare(const gc_ptr<interned_string> &name,
                                         gc_ptr<const record_type> super = nullptr) {
        /*
         * In order to get a non-const one, we need to create one even if there
         * already was one.
         */
        gc_ptr<record_type> rt = make_gc<record_type>(private_ctor{}, name, super);
        gc_ptr<const record_type> old_rt = find(name);
        if (old_rt != nullptr) {
          rt->_forward = old_rt;
          rt->_created = true;
          if (super == nullptr) {
            return rt;
          }
          gc_ptr<const record_type> ect = super->ensure_created();
	  if (ect != nullptr) {
	    super = ect;
	  }
          gc_ptr<const record_type> s = rt->_super;
          /*
           * The asserted super must be a super of the actual super.
           */
          if (!s->is_super_of(super)) {
            // We want a superclass that's incompatible with the one that's in the table
            throw incompatible_superclass_ex{};
          }
        }
        return rt;
      }
      gc_ptr<record_field_base> lookup_field(const gc_ptr<interned_string> &field_name) const {
        if (_forward != nullptr) {
          return _forward->lookup_field(field_name);
        }
        auto from = _fields.begin();
        auto to = _fields.end();
        auto fp = std::find_if(from, to,
                               [=](const gc_ptr<record_field_base> &f) {
          return f->name == field_name;
        });
        if (fp == to) {
          return nullptr;
        } else {
          return *fp;
        }
      }
      template <typename Fn>
      auto add_field(Fn&& creator) {
        if (_created) {
          throw unmodifiable_record_type_ex{};
        }
        /*
         * We assume that we don't have to worry about race conditions here.
         */
        std::size_t index = _fields.size();
        auto f = creator(index);
        _fields.push_back(f);
        // std::cout << "  Added " << f->num << ": " << f->name << " to " << name() << std::endl;
        return f;
      }

      managed_value<managed_record> create_record(const gc_ptr<iso_context> &ctxt) const;

    };



    template <kind K>
    gc_ptr<kind_field<K>>
    managed_type<K>::field_in(const gc_ptr<record_type> &rt,
                              const gc_ptr<interned_string> &name,
                              bool create_if_absent) const {
      gc_ptr<record_field_base> f = rt->lookup_field(name);
      if (f != nullptr) {
        if (same_type_as(f->f_type_base)) {
          return std::static_pointer_cast<kind_field<K>>(f);
        } else {
          throw incompatible_type_ex{};
        }
      }
      /*
       * The field doesn't exist.
       */
      if (!create_if_absent) {
        return nullptr;
      }
      /*
       * add_field() takes a field creator for once the record type has figured out the
       * index.  It will throw unmodifiable_record_type_exception if it can't be done,
       * which we pass through.
       */
      return rt->add_field([=](std::size_t index) {
        gc_ptr<const kind_type<K>> concrete_type = downcast();
        return make_gc<kind_field<K>>(name,index,rt,concrete_type);
      });
    }



    inline
    managed_record::managed_record(gc_token &gc, const gc_ptr<const record_type> &rt)
    : managed_composite(gc), r_type{rt}, fields(make_gc_array<atomic_msv>(rt->n_fields()))
    {}


    inline
    managed_value<managed_record>
    record_type::create_record(const gc_ptr<iso_context> &ctxt) const
    {
      ensure_created();
      if (_forward != nullptr) {
        return _forward->create_record(ctxt);
      }
      gc_ptr<managed_record> r = make_gc<managed_record>(GC_THIS);
      gc_ptr<branch> b = ctxt->shadow(top_level_branch);
      return managed_value<managed_record>{r, b};
    }

    template <kind K>
    gc_ptr<msv<K>>
    managed_record::field(std::size_t i, const gc_ptr<const record_field<K>> &field, bool create_if_null) const {
      atomic_msv &a = fields[i];
      gc_ptr<msv_base> vb = a.load();
      if (vb != nullptr) {
        return vb->downcast<K>();
      } else if (create_if_null) {
        /*
         * Need a non-const version of this in order to create the conflict generator.
         */
        managed_record *nc_this = const_cast<managed_record *>(this);
	auto cg = make_gc<typename field_conflict<K>::generator>(this_as_gc_ptr(nc_this), field);
        gc_ptr<msv<K>> new_msv = make_gc<msv<K>>(cg);
        auto rr = ruts::try_change_value(a, nullptr, new_msv);
        /*
         * If that didn't work, someone else got there first.
         */
        if (rr) {
          return new_msv;
        } else {
//          managed_space::destroy(new_msv);
          return rr.prior_value->template downcast<K>();
        }
      } else {
        return nullptr;
      }
    }

    inline
    bool record_field_base::is_valid() const {
      return check_validity(validity_state,
                            [this](){
        try {
          r_type->ensure_created();
          return true;
        } catch (const incompatible_record_type_ex &) {
          return false;
        }
      });
    }


    template <kind K>
    inline
    kind_mv<K>
    record_field<K>::read(const gc_ptr<const managed_record> &r,
                          const gc_ptr<branch> &b,
                          const gc_ptr<iso_context> &ctxt) const {
//      std::cerr << "Reading field: " << this << ", rec: " << r << ", branch: " << b << ", ctxt: " << ctxt << std::endl;
      if (!is_valid()) {
        throw incompatible_record_type_ex{};
      }
      if (!r_type->is_super_of(r->r_type)) {
        throw incompatible_record_type_ex{};
      }
//      std::cerr << "  It's compatible" << std::endl;
      gc_ptr<msv<K>> val = r->field<K>(num, GC_THIS);
//      std::cerr << "  val: " << val << std::endl;
      if (val == nullptr) {
        return kind_mv<K>{};
      }
      gc_ptr<branch> sb = ctxt->shadow(b);
//      std::cerr << "  shadow branch: " << sb << std::endl;
//      std::cout << "read in " << ctxt << " : " << b << " -> " << sb << std::endl;
      return val->read(sb, ctxt);
    }

    template <kind K>
    inline
    kind_mv<K>
    record_field<K>::read_frozen(const gc_ptr<const managed_record> &r,
                                 const gc_ptr<branch> &b,
                                 const gc_ptr<iso_context> &ctxt) const {
      if (!is_valid()) {
        throw incompatible_record_type_ex{};
      }
      /*
       * TODO: Need a faster way of checking types
       */
      if (!r_type->is_super_of(r->r_type)) {
        throw incompatible_record_type_ex{};
      }
      gc_ptr<msv<K>> val = r->field<K>(num, GC_THIS);
      if (val == nullptr) {
        return kind_mv<K>{};
      }
      gc_ptr<branch> sb = ctxt->shadow(b);
      return val->read_frozen(sb, ctxt);
    }

    template <kind K>
    inline
    bool
    record_field<K>::has_value(const gc_ptr<const managed_record> &r,
                               const gc_ptr<branch> &b,
                               const gc_ptr<iso_context> &ctxt) const {
      if (!is_valid()) {
        throw incompatible_record_type_ex{};
      }
      if (!r_type->is_super_of(r->r_type)) {
        throw incompatible_record_type_ex{};
      }
      gc_ptr<msv<K>> val = r->field<K>(num, GC_THIS);
      if (val == nullptr) {
        return false;
      }
      gc_ptr<branch> sb = ctxt->shadow(b);
      return val->has_value(sb, ctxt);
    }

    template <kind K>
    inline
    kind_mv<K>
    record_field<K>::modify(const gc_ptr<const managed_record> &r,
                            const gc_ptr<branch> &b,
                            const gc_ptr<iso_context> &ctxt,
                            modify_op op, const kind_mv<K> &arg, res_mode resolving) const {
      if (!is_valid()) {
        throw incompatible_record_type_ex{};
      }
      if (!r_type->is_super_of(r->r_type)) {
        throw incompatible_record_type_ex{};
      }
      gc_ptr<msv<K>> val = r->field<K>(num, GC_THIS, true);
      gc_ptr<branch> sb = ctxt->shadow(b);
//      std::cout << "write in " << ctxt << " : " << b << " -> " << sb << std::endl;
      return val->modify(sb, ctxt, op, resolving, arg);

    }



  }
}



#endif /* CORE_RECORD_H_ */
